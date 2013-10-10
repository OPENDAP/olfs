/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.aws.glacier;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierAsyncClient;
import opendap.coreServlet.ServletUtil;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.*;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 9/26/13
 * Time: 9:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class DownloadManager {

    private Logger _log;

    private boolean _isInitialized;

    private ConcurrentHashMap<String, ArchiveDownload> _activeDownloads;

    private WorkerThread worker;
    private Thread workerThread;

    private File _targetDir;

    private String  _activeDownloadBackupFileName;

    private ReentrantLock _downloadLock;

    private static DownloadManager _theManager = null;

    public static final String GLACIER_ARCHIVE_RETRIEVAL = "archive-retrieval";
    public static final String GLACIER_INVENTORY_RETRIEVAL = "inventory-retrieval";

    public static final long DEFAULT_GLACIER_ACCESS_DELAY = 14400; // 4 hours worth of seconds.
    public static final long MINIMUM_GLACIER_ACCESS_DELAY = 60; // 4 hours worth of seconds.

    private AWSCredentials _awsCredentials;

    private DownloadManager() {

        _log = LoggerFactory.getLogger(this.getClass());
        _activeDownloads = new ConcurrentHashMap<String, ArchiveDownload>();
        _downloadLock = new ReentrantLock();
        _activeDownloadBackupFileName = this.getClass().getSimpleName() + "-ActiveDownloads";
        _isInitialized = false;
        _awsCredentials = null;
        workerThread = null;

    }

    public static DownloadManager theManager(){

        if(_theManager ==null)
            _theManager = new DownloadManager();

        return _theManager;
    }


    public void init(File targetDir, AWSCredentials credentials) throws IOException, JDOMException {

        _downloadLock.lock();
        try {
            if(_isInitialized) {
                _log.debug("init() - Already initialized, nothing to do.");
                return;
            }

            _awsCredentials = credentials;

            _targetDir = targetDir;

            loadActiveDownloads();

            startDownloadWorker(_awsCredentials);

            _isInitialized = true;

        }
        finally {
            _downloadLock.unlock();
        }
    }

    public boolean alreadyRequested(GlacierRecord gRec){
        String resourceId = gRec.getResourceId();

        _downloadLock.lock();
        try {
            return _activeDownloads.containsKey(resourceId);
        }
        finally {
            _downloadLock.unlock();
        }


    }

    public long initiateGlacierDownload(GlacierRecord gRec ) throws JDOMException, IOException {

        return initiateGlacierDownload(_awsCredentials,gRec);
    }



    public long initiateGlacierDownload(AWSCredentials glacierCreds, GlacierRecord gRec ) throws JDOMException, IOException {

        _log.debug("initiateGlacierDownload() - BEGIN");

        String vaultName  = gRec.getVaultName();
        String archiveId  = gRec.getArchiveId();
        String resourceId = gRec.getResourceId();

        _log.debug("initiateGlacierDownload() -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -");
        _log.debug("initiateGlacierDownload() - Downloading resource from Glacier:  ");
        _log.debug("initiateGlacierDownload() -     Vault Name:  {}", vaultName);
        _log.debug("initiateGlacierDownload() -     Resource ID: {}", resourceId);
        _log.debug("initiateGlacierDownload() -     Archive ID:  {}", archiveId);

        long estimatedRetrievalTime = DEFAULT_GLACIER_ACCESS_DELAY;
        _downloadLock.lock();
        try {
            AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(glacierCreds);
            client.setEndpoint(GlacierArchiveManager.theManager().getGlacierEndpoint());


            if(_activeDownloads.containsKey(resourceId)){

                ArchiveDownload activeDownload = _activeDownloads.get(resourceId);
                estimatedRetrievalTime = activeDownload.estimatedTimeRemaining();

                if(activeDownload.isReadyForDownload()){
                    activeDownload.download();
                    estimatedRetrievalTime = 0;
                }

            }
            else {

                ArchiveDownload ad = new ArchiveDownload(gRec,glacierCreds, DEFAULT_GLACIER_ACCESS_DELAY);

                if(ad.startArchiveRetrieval()) {
                    _activeDownloads.put(resourceId,ad);
                    saveActiveDownloads();
                    _log.debug("initiateGlacierDownload() - Active downloads saved.");
                    estimatedRetrievalTime = ad.estimatedTimeRemaining();
                }
                else {
                    estimatedRetrievalTime = -1;

                }

            }
        }
        finally {
            _downloadLock.unlock();
        }

        _log.debug("initiateGlacierDownload() - END");


        return estimatedRetrievalTime;
    }



    private void saveActiveDownloads() throws IOException {


        File backup = new File(_targetDir,_activeDownloadBackupFileName);

        FileOutputStream fos = new FileOutputStream(backup);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(_activeDownloads);


    }

    private void loadActiveDownloads() throws IOException {


        File backup = new File(_targetDir,_activeDownloadBackupFileName);

        if(backup.exists()){
            FileInputStream fis = new FileInputStream(backup);
            ObjectInputStream ois = new ObjectInputStream(fis);

            try {
                _activeDownloads = (ConcurrentHashMap<String, ArchiveDownload>) ois.readObject();
            } catch (Exception e) {
                reloadingJobsError(backup,e);

            }
        }

    }

    private void reloadingJobsError(File activeJobsArchive, Exception e) throws IOException {

        String msg =  "Unable to load ArchiveDownload archive: "+activeJobsArchive.getAbsoluteFile()+" Msg: "+e.getMessage();
        _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        _log.error(msg);
        _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        throw new IOException(msg,e);


    }


    private void setTargetDirectory(String targetDirName) throws IOException {

        if(targetDirName==null)
            throw new IOException("Working directory name was null valued.");


        File targetDir = new File(targetDirName);
        if(!targetDir.exists() && !targetDir.mkdirs()){
            throw new IOException("setTargetDirectory(): Unable to create working directory: "+targetDir);
        }
        _targetDir = targetDir;

        _log.info("setTargetDirectory() - Working directory set to {}", _targetDir.getName());

    }


    private void startDownloadWorker(AWSCredentials credentials){

        worker = new WorkerThread(credentials);

        workerThread = new Thread(worker);

        workerThread.setName("DownLoadManager-"+workerThread.getName());
        workerThread.start();
        _log.info("init(): Worker Thread started.");
        _log.info("init(): complete.");

    }




    public void destroy() {
        while(workerThread.isAlive()){
            _log.info("destroy(): "+workerThread.getName()+" isAlive(): "+workerThread.isAlive());

            _log.info("destroy(): Interrupting "+workerThread.getName()+"...");
            workerThread.interrupt();

            _log.info("destroy(): Waiting for "+workerThread.getName()+" to complete ...");

            try {
                workerThread.join();

                if(workerThread.isAlive()){
                    _log.error("destroy(): "+workerThread.getName()+" is still Alive!!.");
                }
                else {
                    _log.info("destroy(): "+workerThread.getName()+" has stopped.");
                }
            } catch (InterruptedException e) {
                _log.info("destroy(): INTERRUPTED while waiting for WorkerThread "+workerThread.getName()+" to complete...");
                _log.info("destroy(): "+workerThread.getName()+" isAlive(): "+ workerThread.isAlive());
            }

        }
        _log.info("destroy(): Destroy completed.");


    }






    class WorkerThread implements Runnable, ServletContextListener {

        private Logger _log;
        private Thread _myThread;
        AWSCredentials _glacierCreds;

        public WorkerThread(AWSCredentials glacierCreds){
            _log = org.slf4j.LoggerFactory.getLogger(getClass());
            _log.info("In WorkerThread constructor.");

            _glacierCreds = glacierCreds;

            _myThread = new Thread(this);
            _myThread.setName("BackgroundWorker" + _myThread.getName());



        }


        public void contextInitialized(ServletContextEvent arg0) {


            ServletContext sc = arg0.getServletContext();

            String contentPath = ServletUtil.getContentPath(sc);
            _log.debug("contentPath: " + contentPath);

            String serviceContentPath = contentPath;
            if(!serviceContentPath.endsWith("/"))
                serviceContentPath += "/";
            _log.debug("_serviceContentPath: " + serviceContentPath);


            _myThread.start();
            _log.info("contextInitialized(): " + _myThread.getName() + " is started.");



        }

        public void contextDestroyed(ServletContextEvent arg0) {

            Thread thread = Thread.currentThread();

            try {
                _myThread.interrupt();
                _myThread.join();
                _log.info("contextDestroyed(): " + _myThread.getName() + " is stopped.");
            } catch (InterruptedException e) {
                _log.debug(thread.getClass().getName() + " was interrupted.");
            }
            _log.info("contextDestroyed(): Finished..");

        }



        public void run() {
            _log.debug("run() - BEGIN");

            long sleepTime = DEFAULT_GLACIER_ACCESS_DELAY;

            Thread thread = Thread.currentThread();
            try {
                while(true && !thread.isInterrupted()){



                    Vector<ArchiveDownload> completed = new Vector<ArchiveDownload>();

                    _downloadLock.lock();
                    try {

                        for(ArchiveDownload activeDownload : _activeDownloads.values()){

                            if(activeDownload.isReadyForDownload()){
                                if(activeDownload.download()){
                                    completed.add(activeDownload);
                                }

                                long wait = activeDownload.estimatedTimeRemaining();

                                if(sleepTime==MINIMUM_GLACIER_ACCESS_DELAY || sleepTime > wait){
                                    sleepTime = wait;
                                }
                                if(sleepTime < MINIMUM_GLACIER_ACCESS_DELAY)
                                    sleepTime = MINIMUM_GLACIER_ACCESS_DELAY;
                            }

                        }

                        for(ArchiveDownload completedDownload: completed){
                            _activeDownloads.remove(completedDownload.getGlacierRecord().getResourceId());
                            saveActiveDownloads();
                        }


                    }
                    finally {
                        _downloadLock.unlock();
                    }

                    _log.info(thread.getName() + "run() -  Sleeping for: " + sleepTime + " seconds");
                    napTime(sleepTime);
                    _log.info(thread.getName() + "run() -  Resetting to: " + sleepTime + " seconds");

                }
            } catch (InterruptedException e) {
                _log.warn("run() - " + thread.getName() + " was interrupted.");
            } catch (Exception e) {
                _log.error(thread.getName() + " Caught " + e.getClass().getName() + "  Msg: " + e.getMessage());
            }
            finally {
                _log.debug("run() - END");

            }
        }


        public void napTime(long intervalInSeconds) throws Exception {
            Thread.sleep(intervalInSeconds * 1000);
            _log.info(Thread.currentThread().getName() + ": Sleep timer expired.");

        }

    }

}
