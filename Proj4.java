import java.io.*;
import java.net.*;
import java.util.*;

/**********************************************************************
 *
 * Project 4: Web Page Preprocessing
 *
 * This program takes a list of one or more URLS from its command
 * line. For each such URL, this program does the following:
 * <ol>
 * <li> Creates a directory.
 * <li> Downloads the resource associated with said URL into its
 * directory
 * <li> If said resource is an HTML file, the program downloads all
 * resources (such as image, audio files, videos) that are necessary
 * to construct a web page. 
 *
 * @author Justin Chan
 * @date 20 April, 2016
 *
 **********************************************************************/

public class Proj4 {

   /**
    * Exit status codes
    */
   private enum exitCodes {NORMAL, USAGE_ERR, OTHER_ERR, MKDIR_ERR};

   /**
    * Disallow the construction of <code>Proj4</code> objects.
    */
   private Proj4() {}

   /**
    * For readability
    */
   public static final String EMPTY_STRING = "";

   /**
    * Say something useful here.
    */
   public static void main(String args[])
   {
      int exitCode = exitCodes.NORMAL.ordinal();
      if (args.length == 0) {
         System.err.println("Usage: java Proj4 url url ...");
         exitCode = exitCodes.USAGE_ERR.ordinal();
      }
      for (String urlString: args) {
         try {
            File directory = mkdir(urlString);
            byte[] argContents = saveResource(directory, urlString,
                                              EMPTY_STRING);
            Vector<String> srcUrls = getSourceUrls(argContents);
            for (String s: srcUrls)
               saveResource(directory, s, urlString);
         }
         catch (Exception ex) {
            System.err.println(ex);
            exitCode = exitCodes.OTHER_ERR.ordinal();
         }
      }
      System.exit(exitCode);
   }

   /**
    * Creates directory name based on hexadecimal of URL address.
    * @param s string of the name of the directory
    * @return the directory, assuming it can be made
    * @throw java.io.IOException if it can't make the directory
    */
   public static File mkdir(String s) throws IOException
   {
     File directory = new File(Integer.toHexString(s.hashCode()));
     directory.mkdir(); //created directory
     return directory;
   }

   /**
    * Save a resource in a file, as well as in a byte array. The
    * latter is the return value.
    * @param dir the directory in which the resources will be saved
    * @param urlString the resource's url, either relative or absolute
    * @param argURLString if not empty, then the urlString was found
    * on the website by argURLString.
    * @return If something goes wrong with the file creation.
    * @throw java.io.Exception if something or another went wrong
    * @throw java.net.URISyntaxException if urlString or argURLString
    * are invalid
    */
   public static byte[] saveResource(File dir, String urlString,
                                     String argURLString)
      throws IOException, URISyntaxException
   {
     byte[] data = null;
     URL url = null;
     File file = null;
     int length = 0;
     int isAbsolute = urlString.indexOf("http://");

     if(isAbsolute == -1)
     {
       url = makeAbsoluteURL(urlString,argURLString);
       file = new File(urlString);
     }
     else
     {	 
       url = new URL(urlString);
       int delete = urlString.lastIndexOf('/',urlString.length());
       String relativeName = urlString.substring(delete+1);
       file = new File(relativeName);
     }
     URI uri = new URI(url.getProtocol(),url.getUserInfo(),
		       url.getHost(),url.getPort(),url.getPath(),
		       url.getQuery(),url.getRef());
     url = uri.toURL();
     URLConnection urlC = url.openConnection();
     InputStream is = null;
     byte[] buffer = new byte[1024];//need a big number
   
     is = urlC.getInputStream();
     //save file in root directory
     FileOutputStream fos = new FileOutputStream(file.toString());
     while(true)
     {
       int len = is.read(buffer);
       if(len == -1)
	 break;
       fos.write(buffer,0,len);
     }
     is.close();
     fos.flush();
     fos.close();
     
     String absoluteDir = dir.getAbsolutePath();
     String newDirPath = absoluteDir + "/" + file.getName();
     File path = new File(newDirPath);
     
     is = new FileInputStream(file);
     fos = new FileOutputStream(path);
     int size;
     //creates a new file in directory
     while((size = is.read(buffer)) > 0)
       fos.write(buffer,0,size);
     is.close();
     fos.close();
     //delete original files
     file.delete();
     
     
     if(argURLString == "")
     {
       ByteArrayOutputStream byteOut = null;
       is = url.openStream();
     
       byteOut = new ByteArrayOutputStream();
     
       while((length = is.read(buffer)) > 0)
	 byteOut.write(buffer,0,length);
       
       data = byteOut.toByteArray();
     }

     return data;
   }  

   /**
    * Get all the src (or SRC) URLs that appear in an array of
    * bytes. For the purpose of this project, we will only look for
    * occurrences of src="stuff" or of SRC="stuff", where stuff is a
    * URL.
    * @param data a sequence of chars that might contain src(or SRC)
    * URLS, initialized from the contents of a URL.
    * @return a Vector containing all the src(or SRC) attributes found
    * in data.
    */
   public static Vector<String> getSourceUrls(byte[] data)
   {
     Vector<String> temp = new Vector<String>();
     String byteToString = new String(data);
     String[]s = byteToString.split("\n");
     for(int i = 0;i < s.length;i++)
     {
       if(makeSrcName(s[i]) != "")
	 temp.add(makeSrcName(s[i]));
     }
     return temp;
   }

   /**
    * Make the absolute version of a relative URL.
    * @param s a relative URL
    * @param argURLString the URL of the web page on which
    * <code>s</code> was found.
    * @return an absolute URL that resolves <code>s</code> and
    * <code>argURLString</code>.
    * @throw java.net.URISyntaxException if urlString is not a valid
    * URL
    * @throw java.net.MalformedURLException if some error occurred
    * when resolving <code>s</code>
    */
   public static URL makeAbsoluteURL(String s, String argURLString)
      throws URISyntaxException, MalformedURLException
   {
     int delete = argURLString.lastIndexOf('/',argURLString.length());
     String newPath = argURLString.substring(0,delete+1);//need '/'
     s = newPath + s;
     return new URL(s);
   }

  /**
   * Case-sensitive version of indexOf().
   * @param s string to search for "src="
   * @return string of index of <code>s</code> substring or "" if "src="
   * not found 
   */
  public static String makeSrcName(String s)
  {
    int search = 0;
    String lcString = s.toLowerCase();
    int found = lcString.indexOf("src=\"");
    int stop = lcString.indexOf("\"",found+5);
    if(found != -1)
    {
      String newAddress = s.substring(found+5,stop);
      return newAddress;
    }
    return "";
  }
}
