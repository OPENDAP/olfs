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


    private ConcurrentHashMap<String, ArchiveDownload>   _activeArchiveDownloads;
    private ConcurrentHashMap<String, InventoryDownload> _activeInventoryDownloads;


    private WorkerThread _worker;
    private Thread _workerThread;

    private File _targetDir;

    private String _activeArchiveDownloadBackupFileName;

    private String _activeInventoryDownloadBackupFileName;


    private ReentrantLock _managerLock;

    private static DownloadManager _theManager = null;

    public static final long DEFAULT_GLACIER_ACCESS_DELAY = 14400; // 4 hours worth of seconds.
    public static final long MINIMUM_GLACIER_ACCESS_DELAY = 60; // 1 minute worth of seconds.

    private AWSCredentials _awsCredentials;

    private DownloadManager() {

        _log = LoggerFactory.getLogger(this.getClass());
        _activeArchiveDownloads = new ConcurrentHashMap<String, ArchiveDownload>();
        _activeInventoryDownloads = new ConcurrentHashMap<String, InventoryDownload>();
        _managerLock = new ReentrantLock();
        _activeArchiveDownloadBackupFileName = this.getClass().getSimpleName() + "-ActiveArchiveDownloads";
        _activeInventoryDownloadBackupFileName = this.getClass().getSimpleName() + "-ActiveInventoryDownloads";
        _isInitialized = false;
        _awsCredentials = null;
        _workerThread = null;

    }

    public static DownloadManager theManager(){

        if(_theManager ==null)
            _theManager = new DownloadManager();

        return _theManager;
    }


    public void init(File targetDir, AWSCredentials credentials) throws IOException, JDOMException {

        _managerLock.lock();
        try {
            if(_isInitialized) {
                _log.debug("init() - Already initialized, nothing to do.");
                return;
            }

            _awsCredentials = credentials;

            _targetDir = targetDir;

            loadActiveArchiveDownloads();
            loadActiveInventoryDownloads();

            _isInitialized = true;

        }
        finally {
            _managerLock.unlock();
        }
    }

    public boolean alreadyRequested(GlacierArchive gRec){
        String resourceId = gRec.getResourceId();

        _managerLock.lock();
        try {
            return _activeArchiveDownloads.containsKey(resourceId);
        }
        finally {
            _managerLock.unlock();
        }


    }

    public long initiateArchiveDownload(GlacierArchive gRec) throws JDOMException, IOException {

        return initiateArchiveDownload(_awsCredentials, gRec);
    }



    public long initiateArchiveDownload(AWSCredentials glacierCreds, GlacierArchive gArc) throws JDOMException, IOException {

        _log.debug("initiateArchiveDownload() - BEGIN");

        String vaultName  = gArc.getVaultName();
        String archiveId  = gArc.getArchiveId();
        String resourceId = gArc.getResourceId();

        _log.debug("initiateArchiveDownload() -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -");
        _log.debug("initiateArchiveDownload() - Downloading resource from Glacier:  ");
        _log.debug("initiateArchiveDownload() -     Vault Name:  {}", vaultName);
        _log.debug("initiateArchiveDownload() -     Resource ID: {}", resourceId);
        _log.debug("initiateArchiveDownload() -     Archive ID:  {}", archiveId);

        long estimatedRetrievalTime = DEFAULT_GLACIER_ACCESS_DELAY;
        _managerLock.lock();
        try {
            AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(glacierCreds);
            client.setEndpoint(GlacierManager.theManager().getGlacierEndpoint());


            if(_activeArchiveDownloads.containsKey(resourceId)){


                if(downloadArchive(resourceId)){
                    estimatedRetrievalTime = 0;
                }
                else {
                    ArchiveDownload download = _activeArchiveDownloads.get(resourceId);
                    estimatedRetrievalTime = download.estimatedTimeRemaining();

                }


            }
            else {

                ArchiveDownload ad = new ArchiveDownload(gArc,glacierCreds, DEFAULT_GLACIER_ACCESS_DELAY);

                if(ad.startJob()) {
                    _activeArchiveDownloads.put(resourceId,ad);
                    saveActiveArchiveDownloads();
                    _log.debug("initiateArchiveDownload() - Active downloads saved.");
                    estimatedRetrievalTime = ad.estimatedTimeRemaining();
                }
                else {
                    estimatedRetrievalTime = -1;

                }

            }
        }
        finally {
            _managerLock.unlock();
        }



        _log.info("initiateArchiveDownload() - Archive Download for resource {} estimated complete in {} seconds.",resourceId,estimatedRetrievalTime);
        _log.debug("initiateArchiveDownload() - END");

        return estimatedRetrievalTime;
    }

    public boolean archiveRetrievalJobCompleted(String downloadId) throws IOException {

        _managerLock.lock();
        try {
            if(_activeArchiveDownloads.containsKey(downloadId)){
                ArchiveDownload download = _activeArchiveDownloads.get(downloadId);
                if(download.jobCompleted()){
                    _log.info("Job '{}' is completed. woot.",downloadId);
                    return true;
                }
                _log.info("Job to retrieve '{}' not yet completed.",downloadId);
            }
            else {
                _log.warn("Download ID '{}' was not found in the active downloads list.",downloadId);
            }
        }
        finally {
            _managerLock.unlock();
        }
        return false;

    }




    public boolean downloadArchive(String resourceId) throws IOException {

        _managerLock.lock();
        try {
            if(_activeArchiveDownloads.containsKey(resourceId)){
                ArchiveDownload archiveDownload = _activeArchiveDownloads.get(resourceId);

                if(archiveDownload.downloadJobOutput()){
                    _log.info("Download job output for resource {} saved.",resourceId);
                    _activeArchiveDownloads.remove(resourceId);
                    saveActiveArchiveDownloads();
                    _log.info("Job to retrieve the archive for resource {} is completed.",resourceId);

                    return true;
                }


                _log.info("Job to retrieve resource '{}' not yet completed.",resourceId);
            }
            else {
                _log.warn("Download ID '{}' was not found in the active downloads list.",resourceId);
            }
        }
        finally {
            _managerLock.unlock();
        }
        return false;

    }




    public long initiateVaultInventoryDownload(String vaultName, String endpointUrl, AWSCredentials glacierCreds) throws JDOMException, IOException {

        _log.debug("initiateVaultInventoryDownload() - BEGIN");

        String downloadFileName = vaultName + Vault.INVENTORY_SUFFIX;
        long estimatedRetrievalTime = DEFAULT_GLACIER_ACCESS_DELAY;

        _managerLock.lock();
        try {


            if(_activeInventoryDownloads.containsKey(vaultName)){

                if(downloadInventory(vaultName)) {
                    estimatedRetrievalTime = 0;
                }
                else {
                    InventoryDownload inventoryDownload = _activeInventoryDownloads.get(vaultName);
                    estimatedRetrievalTime = inventoryDownload.estimatedTimeRemaining();
                    _log.info("initiateVaultInventoryDownload() - Inventory download for vault {} not yet completed. Estimated time remaining: {} seconds",vaultName,estimatedRetrievalTime);
                }

            }
            else {

                InventoryDownload inventoryDownload = new InventoryDownload(vaultName,endpointUrl,glacierCreds,DEFAULT_GLACIER_ACCESS_DELAY);
                if(inventoryDownload.startJob()) {
                    inventoryDownload.setDownloadFile(new File(_targetDir,downloadFileName));
                    _activeInventoryDownloads.put(vaultName, inventoryDownload);
                    saveActiveInventoryDownloads();
                    _log.info("initiateVaultInventoryDownload() - Active inventory downloads saved.");
                    estimatedRetrievalTime = inventoryDownload.estimatedTimeRemaining();
                }
                else {
                    estimatedRetrievalTime = -1;

                }

            }
        }
        finally {
            _managerLock.unlock();
        }

        _log.debug("initiateVaultInventoryDownload() - END");


        return estimatedRetrievalTime;
    }

    public boolean inventoryRetrievalJobCompleted(String vaultName) throws IOException {

        _managerLock.lock();
        try {
            if(_activeInventoryDownloads.containsKey(vaultName)){
                InventoryDownload download = _activeInventoryDownloads.get(vaultName);
                if(download.jobCompleted()){
                    _log.info("Inventory retrieval job for vault '{}' is completed. woot.",vaultName);
                    return true;
                }
                _log.info("Job to retrieve the inventory for vault {} has not yet completed.",vaultName);
            }
            else {
                _log.warn("There is no active job to retrieve an inventory for vault {}.",vaultName);
            }
        }
        finally {
            _managerLock.unlock();
        }
        return false;

    }


    public boolean downloadInventory(String vaultName) throws IOException {

        _managerLock.lock();
        try {

            if(_activeInventoryDownloads.containsKey(vaultName)){
                InventoryDownload inventoryDownload = _activeInventoryDownloads.get(vaultName);
                if(inventoryDownload.downloadJobOutput()){
                    _log.info("Inventory job output for vault {} saved.",vaultName);
                    _activeInventoryDownloads.remove(vaultName);
//                    saveActiveInventoryDownloads();
                    _log.info("Job to retrieve the inventory for vault {} is completed.",vaultName);

                    return true;
                }
                else {
                    _log.error("ERROR! Failed to download the inventory job output for vault {}.", vaultName);
                }
            }
            else {
                _log.warn("There is no active job to retrieve an inventory for vault {}.", vaultName);
            }
        }
        finally {
            _managerLock.unlock();
        }
        return false;

    }



    private void saveActiveInventoryDownloads() throws IOException {


        File backup = new File(_targetDir, _activeInventoryDownloadBackupFileName);

        FileOutputStream fos = new FileOutputStream(backup);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(_activeInventoryDownloads);


    }

    private void loadActiveInventoryDownloads() throws IOException {


        File backup = new File(_targetDir, _activeInventoryDownloadBackupFileName);

        if(backup.exists()){
            FileInputStream fis = new FileInputStream(backup);
            ObjectInputStream ois = new ObjectInputStream(fis);

            try {
                _activeInventoryDownloads = (ConcurrentHashMap<String, InventoryDownload >) ois.readObject();
            } catch (Exception e) {
                reloadingJobsError(backup,e);

            }
        }

    }



    private void saveActiveArchiveDownloads() throws IOException {


        File backup = new File(_targetDir, _activeArchiveDownloadBackupFileName);

        FileOutputStream fos = new FileOutputStream(backup);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(_activeArchiveDownloads);


    }

    private void loadActiveArchiveDownloads() throws IOException {


        File backup = new File(_targetDir, _activeArchiveDownloadBackupFileName);

        if(backup.exists()){
            FileInputStream fis = new FileInputStream(backup);
            ObjectInputStream ois = new ObjectInputStream(fis);

            try {
                _activeArchiveDownloads = (ConcurrentHashMap<String, ArchiveDownload>) ois.readObject();
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


    public void startDownloadWorker(AWSCredentials credentials){

        _managerLock.lock();
        try {
            if(_worker==null) {
                _worker = new WorkerThread(credentials);
            }
            else {
                _log.warn("startDownloadWorker(): WorkerThread object all ready instantiated. Will not make a new one...");

            }

            if(_workerThread==null){

                _workerThread = new Thread(_worker);

                _workerThread.setName("DownLoadManager-" + _workerThread.getName());
                _workerThread.start();
                _log.info("startDownloadWorker(): Worker Thread started.");
                _log.info("startDownloadWorker(): complete.");
            }
            else {
                _log.warn("startDownloadWorker(): Worker thread all ready exists. Will not make a new one...");

            }

        }
        finally {
            _managerLock.unlock();
        }
    }


    public void startDownloadWorker(){

        startDownloadWorker(_awsCredentials);

    }




    public void destroy() {
        _managerLock.lock();

        try {
            if(_workerThread!=null){
                while(_workerThread.isAlive()){
                    _log.info("destroy(): "+ _workerThread.getName()+" isAlive(): "+ _workerThread.isAlive());

                    _log.info("destroy(): Interrupting "+ _workerThread.getName()+"...");
                    _workerThread.interrupt();

                    _log.info("destroy(): Waiting for "+ _workerThread.getName()+" to complete ...");

                    try {
                        _workerThread.join();

                        if(_workerThread.isAlive()){
                            _log.error("destroy(): "+ _workerThread.getName()+" is still Alive!!.");
                        }
                        else {
                            _log.info("destroy(): "+ _workerThread.getName()+" has stopped.");
                        }
                    } catch (InterruptedException e) {
                        _log.info("destroy(): INTERRUPTED while waiting for WorkerThread "+ _workerThread.getName()+" to complete...");
                        _log.info("destroy(): "+ _workerThread.getName()+" isAlive(): "+ _workerThread.isAlive());
                    }

                }
                _workerThread = null;
                _worker = null;
            }
        }
        finally {
            _managerLock.unlock();
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

            String configPath = ServletUtil.getConfigPath(sc);
            _log.debug("configPath: " + configPath);

            String serviceConfigPath = configPath;
            if(!serviceConfigPath.endsWith("/"))
                serviceConfigPath += "/";
            _log.debug("_serviceConfigPath: " + serviceConfigPath);


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



                    Vector<Download> completed = new Vector<Download>();

                    _managerLock.lock();
                    try {

                        for(Download download : _activeArchiveDownloads.values()){

                            if(download.jobCompleted()){
                                if(download.downloadJobOutput()){
                                    completed.add(download);
                                }

                                long wait = download.estimatedTimeRemaining();

                                if(sleepTime==MINIMUM_GLACIER_ACCESS_DELAY || sleepTime > wait){
                                    sleepTime = wait;
                                }
                                if(sleepTime < MINIMUM_GLACIER_ACCESS_DELAY)
                                    sleepTime = MINIMUM_GLACIER_ACCESS_DELAY;
                            }

                        }

                        for(Download completedDownload: completed){
                            _activeArchiveDownloads.remove(completedDownload.getDownloadId());
                            saveActiveArchiveDownloads();
                        }


                    }
                    finally {
                        _managerLock.unlock();
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
