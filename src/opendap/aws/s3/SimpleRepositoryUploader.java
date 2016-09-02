package opendap.aws.s3;

import opendap.noaa_s3.S3Index;
import org.apache.commons.cli.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by ndp on 8/31/16.
 */
public class SimpleRepositoryUploader {

    private SimpleS3Uploader _simpleS3Uploader;

    private static final String s3Url = "https://s3.amazonaws.com/";
    private  String _delimiter = "/";
    private  String _base = null;

    private boolean _verbose;
    private boolean _dryRun;

    private String _s3BucketName;
    private String _awsAccessKeyId;
    private String _awsSecretKey;
    private String _repoDirectoy;
    private boolean _makePubliclyReadable;



    private  boolean processCommandline(String[] args) throws Exception {

        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("h", "help", false, "Usage information.");
        options.addOption("p", "public", false, "Makes all of the files uploaded publicly readable");
        options.addOption("v", "verbose", false, "Makes more output...");
        options.addOption("t", "dry-run", false, "Does everything but actually push stuff into S3 ");

        options.addOption("i", "awsId", true, "AWS access key ID for working with S3.");
        options.addOption("s", "awsKey", true, "AWS secret key for working with S3.");
        options.addOption("b", "s3-bucket-name", true, "Name of S3 bucket on which to operate.");
        options.addOption("d", "directory", true, "The fully qualified path to the top directory of " +
                "the tree you want to ingest into S3..");


        CommandLine line =   parser.parse(options, args);

        String usage  = this.getClass().getSimpleName()+" [-h] [-p] [-v] [-t] -i AWSAccessKeyID -s AWSSecretKey -b S3BucketName -d repoDirectoryRoot ";

        StringBuilder errorMessage = new StringBuilder();

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return false;
        }

        _verbose = line.hasOption("verbose");
        System.out.println("Verbose mode: "+(_verbose?"ENABLED":"DISABLED"));

        _dryRun = line.hasOption("dry-run");
        System.out.println("Dry-Run mode: "+(_dryRun?"ENABLED":"DISABLED"));

        _makePubliclyReadable = line.hasOption("public");
        System.out.println("Access: "+(_makePubliclyReadable?"Public":"Private"));

        /*
        s3LocalCacheRoot = line.getOptionValue("s3-root");
        if(s3LocalCacheRoot==null){
            errorMessage.append("Missing Parameter - You must provide an S3 cache directory with the --s3-root option.\n");
        }
        */

        _s3BucketName = line.getOptionValue("s3-bucket-name");
        if(_s3BucketName ==null){
            errorMessage.append("Missing Parameter - You must provide a S3 bucket name with the --s3-bucket-name option.\n");
        }
        _base = s3Url + _s3BucketName;


        _awsAccessKeyId =  line.getOptionValue("awsId");
        if(_awsAccessKeyId == null){
            errorMessage.append("Missing Parameter - You must provide an AWS access key ID (to access the S3 service) with the --awsId option.\n");
        }

        _awsSecretKey = line.getOptionValue("awsKey");
        if(_awsSecretKey == null){
            errorMessage.append("Missing Parameter - You must provide an AWS secret key (to access the S3 service) with the --awsKey option.\n");
        }

        _repoDirectoy = line.getOptionValue("directory");
        if (_repoDirectoy ==null) {
            errorMessage.append("Missing Parameter - You must provide an source directory (with the -d option) from which to build the S3 repo.\n");
        }

        if(errorMessage.length()!=0){
            System.err.println(errorMessage);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return false;
        }


       _simpleS3Uploader = new SimpleS3Uploader(_s3BucketName,_awsAccessKeyId,_awsSecretKey);


        return true;

    }

    public static void main(String[] args) {

        try {
            SimpleRepositoryUploader srb = new SimpleRepositoryUploader();
            if(srb.processCommandline(args)){
                File f = new File(srb._repoDirectoy);
                srb.s3Ingest(f);
            }
        }
        catch (Throwable t){
            System.err.println("Caught "+t.getClass().getName()+"  Message: "+t.getMessage());
            t.printStackTrace(System.err);
        }
    }







    /**
     * Builds an index for the passed directory. If the passed File is not a directory or cannot be
     * This is an example of the index file that will be built.
     *
    <?xml version="1.0" encoding="UTF-8"?>
    <?xml-stylesheet type='text/xsl' href='/opendap.test//index.xsl'?>
    <index xmlns="http://nodc.noaa.gov/s3/catalog/1.0" base="https://s3.amazonaws.com/opendap.test" path="" name="opendap.test" delimiter="/" encoding="UTF-8">
      <folder name="data" size="231402720" count="1"/>
    </index>
     *
     *
     * @param node The directory to index.
     * @param base The value of the base attribute for this index.
     * @param parentPath  The index/catalog path to the parent directory.
     * @return The index document object.
     * @throws BadIngestException When the passed file is not a directory or is not readable.
     */
    public Document mkIndexDoc(File node, String base, String parentPath) throws BadIngestException {


        if(!node.isDirectory())
            throw new BadIngestException("mkIndexDoc() - The file "+node.getName()+" is not a directory");

        if(!node.canRead())
            throw new BadIngestException("mkIndexDoc() - The directory "+node.getName()+" cannot be read");

        String path = _delimiter + pathConcat(parentPath , node.getName());


        Element index = new Element("index", S3Index.S3_CATALOG_NAMESPACE);
        index.setAttribute("base",base);
        index.setAttribute("name",_s3BucketName);
        index.setAttribute("path",path);
        index.setAttribute("delimiter",_delimiter);
        index.setAttribute("encoding","UTF-8");


        for(File f: node.listFiles()){
            Element indexElement;
            if(f.isDirectory()){
                indexElement = new Element("folder",S3Index.S3_CATALOG_NAMESPACE);
            }
            else {
                indexElement = new Element("file",S3Index.S3_CATALOG_NAMESPACE);
            }

            indexElement.setAttribute("name",f.getName());
            Date lmt = new Date(f.lastModified());
            SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            indexElement.setAttribute("last-modifed",outputFormatter.format(lmt));
            indexElement.setAttribute("size",Long.toString(f.length()));
            index.addContent(indexElement);

        }

        Document indexDoc = new Document();

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", "/opendap.test//index.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        indexDoc.addContent(pi);

        indexDoc.setRootElement(index);

        return indexDoc;
    }


    /**
     * Beginning with {@code startingPoint} this recursive method crawls the filesystem building index files to
     * represent each directory. These index files along with all of the regular files encountered in the crawl
     * are uploaded to the S3 bucket identified through the {@code SimpleRepositoryUploader} object's state.
     * The resulting index.xml files in S3 will provide the same logical view of the regular files as was
     * manifested by the file system from which they were harvested.
     *
     * @param startingPoint The staring pint directry/file for the crawl.
     * @throws BadIngestException
     * @throws IOException
     */
    public void s3Ingest(File startingPoint) throws IOException, BadIngestException {
        s3Ingest(startingPoint, "",_makePubliclyReadable,0);
    }




    /**
     * Beginning with {@code startingPoint} this recursive method crawls the filesystem building index files to
     * represent each directory. These index files along with all of the regular files encountered in the crawl
     * are uploaded to the S3 bucket identified through the {@code SimpleRepositoryUploader} object's state.
     * The resulting index.xml files in S3 will provide the same logical view of the regular files as was
     * manifested by the file system from which they were harvested.
     *
     * @param startingPoint The staring pint directry/file for the crawl.
     * @param parentPath The S3 navigation path tot he parent directory (typically set to "" at the begining of crawl)
     * @param makePubliclyReadable If true the objects uploaded to S3 will be publicly readable. The default
     *                             is false/private.
     * @param level This is a recurisive level counter -
     * @throws BadIngestException
     * @throws IOException
     */
    public void s3Ingest(File startingPoint, String parentPath, boolean makePubliclyReadable, int level) throws BadIngestException, IOException {

        String myPath = pathConcat(parentPath , startingPoint.getName());

        System.out.println("");
        System.out.println(level + " ----------------------------------------------------------- "+level);
        System.out.println("ITEM: " + myPath + " (" + (startingPoint.isDirectory()?"dir":"file") + ")");
        if (startingPoint.isDirectory()) {
            System.out.println("- - - - - - - - - - - - - - - - -");
            System.out.println("Building Index File for "+ myPath);
            Document index = mkIndexDoc(startingPoint,_base,parentPath);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            String s3IndexId = pathConcat(myPath,"/index.xml");
            System.out.println("Index File s3Id: "+s3IndexId+"\n");
            System.out.print(xmlo.outputString(index));

            File tmpIndexFile = new File("/tmp/s3IndexTmpFile.xml");
            if(tmpIndexFile.exists() && tmpIndexFile.canWrite()){
                if(!tmpIndexFile.delete())
                    throw new IOException("Unable to delete file: "+tmpIndexFile.getCanonicalPath());
            }
            if(tmpIndexFile.createNewFile()){
                FileOutputStream fos  = new FileOutputStream(tmpIndexFile);
                xmlo.output(index,fos);
                fos.close();
                System.out.println("[S3 INDEX UPLOAD POINT] fileName: " + tmpIndexFile.getCanonicalPath() +
                        "  s3IndexId: " + s3IndexId );
                if(!_dryRun) {
                    _simpleS3Uploader.uploadFile(tmpIndexFile,s3IndexId,makePubliclyReadable);
                }
                else {
                    System.out.println("** [DRY-RUN] NO S3 ACTION TAKEN");

                }
                System.out.println("Index creation and transmission completed.");

            }
            for(File child : startingPoint.listFiles()){
                if(!child.isDirectory()){
                    if(startingPoint.canRead()){
                        String s3Id = pathConcat(myPath,child.getName());
                        System.out.println("- - - - - - - - - - - - - - - - -");
                        System.out.println("[S3 FILE UPLOAD POINT] fileName: "+child.getCanonicalPath()+"  s3Id: "+s3Id);
                        if(!_dryRun) {
                            System.out.println("Uploading file: "+child.getCanonicalPath()+ "  to S3 bucket "+_s3BucketName);
                            _simpleS3Uploader.uploadFile(child,s3Id,makePubliclyReadable);
                        }
                        else {
                            System.out.println("** [DRY-RUN] NO S3 ACTION TAKEN");

                        }
                        System.out.println("File transmission completed.");
                    }
                    else {
                        throw new BadIngestException("Unable to read file: "+child.getName());
                    }
                }
                else {
                    // Recursive index generation
                    s3Ingest(child,myPath, makePubliclyReadable, level+1);
                }
            }
        }

    }

    private  String pathConcat(String parentPath, String child){

        while(parentPath.endsWith(_delimiter)){
            parentPath = parentPath.substring(0,parentPath.length()-1);
        }
        if(parentPath.isEmpty())
            return child;
        else
            return parentPath + _delimiter + child;
    }

}
