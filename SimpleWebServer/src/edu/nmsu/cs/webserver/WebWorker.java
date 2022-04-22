package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.Base64.Encoder;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.lang.model.util.ElementScanner14;

import java.awt.image.BufferedImage;

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			// String respCode = "";
			String desiredFilePath = readHTTPRequest(is);


			File file = new File(desiredFilePath); // test to see what file is
			File imageFile = new File(desiredFilePath);
			
			//if (getExtension(file).equals("image/png") || getExtension(file).equals("image/jpeg") || getExtension(file).equals("image/gif") )
				
				

			System.err.println("THE FILE IS THERE: " + file.exists()); 
			System.err.println("THIS IS THE FILE TYPE: " + getExtension(file)); // test to get file extension (this will help with p2)
			System.err.println("THIS IS WANT FILE: " + desiredFilePath);  // test for desired file
			System.err.println("IS IMAGE: " + isImageFile(file));



                               // This is LITERALLY the content type!!!!
			writeHTTPHeader(os, "text/html", findFile(desiredFilePath)); // findFile should return the resp code array




			writeContent(os, findFile(desiredFilePath), desiredFilePath, file);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}



	public String getExtension(File inputFile) {

		String fileExten = "";
		try {
			fileExten = Files.probeContentType(inputFile.toPath());
		} catch (IOException e) {
			System.err.println("ERROR: Unable to get file type.");
		}	
		return fileExten;
	}



	/**
	 * 
	 * @param filepath
	 * @return
	 * 
	 */
	public String findFile(String filePath) {
		File html_file = new File(filePath);
		

		if(getExtension(html_file).contains("plain"))
			return "400";

		else if(html_file.exists()  )
			return "200";

		else
			return "404";
	}

	public boolean isImageFile(File file) {

		if (getExtension(file).equals("image/jpeg") ||getExtension(file).equals("image/png") )
			return true;

		return false;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is)
	{
		String line;
		String filePath = ""; // created filepath
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");
				if (line.contains("GET")) {
					filePath = line.split(" ")[1];
				
					if (filePath.equals("/"))
						filePath = "/hello.html";
					
					filePath = filePath.substring(1);
					return filePath;
				}	

				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return filePath;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 * 
	 * NOTE this is the second to last step....this is done AFTER GET request 
	 * has been processed, whether or not the file was found.
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, String resCode) throws Exception 
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		if (resCode.equals("200"))
			os.write("HTTP/1.1 200 OK\n".getBytes()); // this if only it works and file exists *****
		else if (resCode.equals("400"))
			os.write("HTTP/1.1 400 Bad Request".getBytes());
		else
			os.write("HTTP/1.1 404 Not Found".getBytes());
			
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, String resCode, String file, File servFile) throws Exception
	{
			
		// if file was found and allowed
		if(resCode.equals("200") && isImageFile(servFile) != true) {

			try {
				Date date = new Date();
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				System.err.println("DATE DUBUG: " + date.toString());
				
				while( line != null) {
					line = line.replace("<cs371date>", date.toString());
					line = line.replace("<cs371server>", "ThisTookLongerThanNeeded");
					os.write(line.getBytes());
					line = reader.readLine();
					
				}
				reader.close();
			} catch (IOException e) {
				System.err.println("Error reading in file.");
			} 
		}
		else if (resCode.equals("200") && isImageFile(servFile) == true) {
			BufferedImage image = ImageIO.read(servFile);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, "png", output);
			String servImage = "<img src=\"" + servFile + "\" alt = \"fish\"";
			os.write(servImage.getBytes()); 
			
		}
		else if (resCode.equals("400")) {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>ERROR 400 BAD REQUEST! </h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}
		else {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>ERROR 404 NOT FOUND! </h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}	
	}

} // end class
