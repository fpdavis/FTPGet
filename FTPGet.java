import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class FTPGet {

	public static void main(String[] args) {

		if (args.length == 0) {
			System.out.println("Usage: FTPGet [ftp|sftp]://[UserName:Password@]Host[:Port]/[Path/]FileName [SavePath/FileName]");
			return;
		}
		
		URI oURI;
		try {
			oURI = ParseURI(args[0]);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}

		String sSavePath;
		if (args.length > 1) {
			sSavePath = args[1];
		}
		else {
			sSavePath = Paths.get(oURI.getPath()).getFileName().toString();
		}
		
		long startTime = System.currentTimeMillis();
		Boolean bSuccess = false;
		if (oURI.getScheme().toLowerCase().equals("sftp")) {
			bSuccess = DownloadViasFTP(oURI, sSavePath);
		}
		else {
			bSuccess = DownloadViaURLConnection(oURI, sSavePath);
		}
		
		if (bSuccess) {
			long endTime = System.currentTimeMillis();
			System.out.println("Download time: " + (endTime - startTime) / 1000 + " seconds");
		}

		System.out.println("Done.");
	}

	private static URI ParseURI(String sURI) throws URISyntaxException {
	
		URI oURI = new URI(sURI);

		
		
		System.out.println("   Schema: " + oURI.getScheme());
		System.out.println("     Host: " + oURI.getHost());
		
		if (oURI.getPort() > 0 ) {
			System.out.println("     Port: " + oURI.getPort());
		}
		else {
			System.out.println("     Port: Default");
		}
		
		if (oURI.getUserInfo() != null && oURI.getUserInfo().contains(":")) {
			String[] aUserInfo = oURI.getUserInfo().split(":");
			System.out.println("     User: " + aUserInfo[0]);
			System.out.println(" Password: " + aUserInfo[1]);
		}
		
		System.out.println("     Path: " + oURI.getPath());
		
		System.out.println("File Name: " + Paths.get(oURI.getPath()).getFileName());
		System.out.println();
					
		return (oURI);
	}
	
 	private static boolean DownloadViasFTP(URI oURI, String sSavePath) {
		
		// Example code from https://www.baeldung.com/java-file-sftp
		
		try {
			ChannelSftp oChannelSftp = setupJsch(oURI);
			oChannelSftp.connect();

			System.out.println("Downloading " + oURI.getPath() + " to " + sSavePath);
			
			oChannelSftp.get(oURI.getPath(), sSavePath);
			oChannelSftp.exit();
		} catch (JSchException e) {
			e.printStackTrace();
			return false;
		} catch (SftpException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	private static ChannelSftp setupJsch(URI oURI) {

		try {
			java.util.Properties oProperties = new java.util.Properties();
			oProperties.put("StrictHostKeyChecking", "no");
			JSch.setConfig(oProperties);

			// Create this file via command line: ssh-keyscan -H -t rsa ftp.umpublishing.org
			// >> C:\Temp\known_hosts.txt
			// JSch.setKnownHosts("C:/Temp/known_hosts.txt");

			String[] aUserInfo = oURI.getUserInfo().split(":");	
			
			Session oJschSession = new JSch().getSession(aUserInfo[0], oURI.getHost());
			oJschSession.setPassword(aUserInfo[1]);
			
			if (oURI.getPort() > 0 ) {
				oJschSession.setPort(oURI.getPort());
			}
			
			System.out.println("Connecting to sFTP server " + oJschSession.getHost() + " on port " + oJschSession.getPort());
			oJschSession.connect();
			return (ChannelSftp) oJschSession.openChannel("sftp");
		} catch (JSchException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static boolean DownloadViaURLConnection(URI oURI, String sSavePath) {

		System.out.println("Connecting to FTP server " + oURI.getHost());

		try {
			URLConnection oURLConnection = oURI.toURL().openConnection();

			InputStream oInputStream = oURLConnection.getInputStream();

			System.out.println("Downloading " + oURI.getPath() + " (" + oURLConnection.getContentLength() / 1024 + " KB) to " + sSavePath);

			FileOutputStream oFileOutputStream = new FileOutputStream(sSavePath);

			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			while ((bytesRead = oInputStream.read(buffer)) != -1) {
				oFileOutputStream.write(buffer, 0, bytesRead);
			}

			oFileOutputStream.close();
			oInputStream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}
}
