package transformer;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.filefilter.FileFileFilter;

import javax.swing.filechooser.FileSystemView;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlTransformer {
	private String sDirectoryXmlSource=null;
	private String sDirectoryXsltOutput=null;
	
	public XmlTransformer(String sDirectoryXPage)throws Exception{	
		this.setDirectoryXmlSource(sDirectoryXPage);
	}
	
	public boolean start() throws Exception{
		boolean bReturn = false;
		main:{
			
			System.out.println("XSLT\n");
			System.out.println("Preparing...");

			//HIER absolute Verzeichnisse verwenden			
			//### Setup directories			
			File fileDirXPage = new File(this.getDirectoryXmlSource());
			String sP = new String(this.getDirectoryXmlSource());
			
			FileSystemView objFileSystem = FileSystemView.getFileSystemView();
			File[] fileaXPage = objFileSystem.getFiles(fileDirXPage, false); 
			
		
			if(null == fileaXPage) break main;
			if(fileaXPage.length==0) break main;
			
			//Get styles			
			File fileDirParent = new File(sP.substring(0, sP.lastIndexOf(System.getProperty("file.separator"))));
			File fileDirStyle = new File(fileDirParent, "xsltInput");
			File[] fileaStyle = objFileSystem.getFiles(fileDirStyle, false);
			
			//#####################################################
			//NUR STYLESCHEETS
			ArrayList<File> listaStyle = new ArrayList<File>();
			for(File file : fileaStyle){
				String sFileName = file.getName();
				if(sFileName.toLowerCase().endsWith(".xsl")|sFileName.toLowerCase().endsWith(".xslt")){
					listaStyle.add(file);
				}
			}
			File[]fileaStyleOnly = listaStyle.toArray(new File[listaStyle.size()]);
						
			if(null == fileaStyleOnly) throw new Exception("No Styles available (null) at '" + fileDirStyle.getAbsolutePath());	
			if(fileaStyleOnly.length==0) throw new Exception("No Styles available (0) at '" + fileDirStyle.getAbsolutePath());
			
			//... sort by name
			Arrays.sort(fileaStyleOnly, NameFileComparator.NAME_COMPARATOR);
			System.out.println("\nStyle FilesNames, case sensitive ascending order (NAME_COMPARATOR)");
			displayFiles(fileaStyleOnly);
		
			
			//#################################
			//The Results
			FileUtils.deleteDirectory(new File(fileDirParent, "xmlOutput"));
			File fileDirOut = new File(fileDirParent, "xmlOutput");
			fileDirOut.mkdirs();
			this.setDirectoryOutput(fileDirOut.getAbsolutePath());
		
		
			//... step - Directories
			int iStep = 0;
			for(File file : fileaStyleOnly){
				iStep++;
				String sPathNew = this.getDirectoryOutput() + File.separator + "xsltStep" + (iStep);
				File fileDirStep = new File(sPathNew);
				fileDirStep.mkdir();
			}
			
			
			//For all Styles
			int iRun = 0;
			boolean bResult = false;
			for(File fileStyle : fileaStyleOnly){
				iRun++;
				bResult = this.transformFilesOnStyle(fileStyle, fileaXPage, iRun);
				if(bResult==false) break main;
			}
			
			bReturn = true;
		}//end main
		return bReturn;		
	}
	
	public boolean transformFilesOnStyle(File fileStyle, File[] fileaXPage, int iRun)throws Exception{
		boolean bReturn = false;
		main:{
			if(fileStyle==null) throw new Exception("No Style File provided.");
			if(fileStyle.exists()==false) throw new Exception("File Style does not exist: '" + fileStyle.getAbsolutePath() + "'");
			
			if(fileaXPage==null) break main;
			if(fileaXPage.length==0) break main;
			
			//#####################################################
			//NUR XML Dateien
			ArrayList<File> listaFile = new ArrayList<File>();
			for(File file : fileaXPage){
				String sFileName = file.getName();
				if(sFileName.toLowerCase().endsWith(".xml")){
					listaFile.add(file);
				}
			}
			File[]fileaXPageOnly = listaFile.toArray(new File[listaFile.size()]);
			
			//#####################################################
			//GGF. ZWISCHENSTEP ERGBNISSE NUTZEN
			File[]fileaXPageUsed = new File[fileaXPageOnly.length];
			if(iRun>=2){
				//verwende die vorherigen Ergebnisse
				int icount = -1;
				for(File fileXPage : fileaXPageOnly){
					icount++;
					String sPathNew = this.getDirectoryOutput() + File.separator + "xsltStep" + (iRun-1) + File.separator + fileXPage.getName();
					File fileNew = new File(sPathNew);
					fileaXPageUsed[icount] = fileNew;
				}
			}else{
				fileaXPageUsed = fileaXPageOnly.clone();
			}

			//######################################################
			//VERARBEITUNG
			System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++");
			System.out.println("Stylesheet XSL: (" + fileStyle.getName() + ")");
			
			boolean bResult = false;
			for(File fileXPage : fileaXPageUsed){
				bResult = this.transformFileOnStyle(fileStyle, fileXPage, iRun);
				if(bResult = false) throw new Exception("Error transforming file '" + fileXPage.getAbsolutePath() + "' on Style '" + fileStyle.getAbsolutePath() + "'");			
			}
						
			bReturn = true;
		}//end main		
		return bReturn;
	}
	
	public boolean transformFileOnStyle(File fileXslt, File fileXml, int iRun)throws Exception{
		boolean bReturn = false;
		
		main:{
			if(fileXslt==null) throw new Exception("No Style File provided.");
			if(fileXslt.exists()==false) throw new Exception("File Style does not exist: '" + fileXslt.getAbsolutePath() + "'");
			
			if(fileXml==null) break main;
			if(fileXml.exists()==false) break main;
			
			System.out.println("\n");
			System.out.println("Input: XML (" + fileXml.getAbsolutePath() + ")");			
			String sPathNew = this.getDirectoryOutput() + File.separator + "xsltStep" + (iRun) + File.separator + fileXml.getName();
			File fileXPageNew = new File(sPathNew);			
			System.out.println("Output: XML (" + fileXPageNew + ")");
			
			System.out.print("Transforming...");

			StreamSource xmlSource = new StreamSource(fileXml);
			StreamSource xsltSource = new StreamSource(fileXslt);
				
			OutputStream out = new java.io.FileOutputStream(fileXPageNew);
				
			Result xmlResultStream = new StreamResult(new java.io.BufferedOutputStream(out));
			TransformerFactory transFact = TransformerFactory.newInstance();
			Transformer trans = transFact.newTransformer(xsltSource);

			trans.transform(xmlSource, xmlResultStream);
			
			out.close();
				
			System.out.print(" Success!");
			
												
			bReturn = true;
		}// end main
		return bReturn;
	}
	
	
	//####### Utils ################
	public static void displayFiles(File[] files) {
		for (File file : files) {
			System.out.printf("File: %-20s Last Modified:" + new Date(file.lastModified()) + "\n", file.getName());
		}
	}
	
	
	//####### GETTER / SETTER #################################
	public String getDirectoryXmlSource(){
		return this.sDirectoryXmlSource;
	}
	public void setDirectoryXmlSource(String sDirectory)throws Exception{
		if(sDirectory==null)throw new Exception("Directory must not be null.");
		if(sDirectory.equals("")) throw new Exception("Directoy must not be empty.");
		
		File file = new File(sDirectory);
		if(file.exists()==false) throw new Exception("Directoy does not exist: '" + file.getAbsolutePath() + "'");
		if(file.isDirectory()==false)throw new Exception("This is not a directory: '" + file.getAbsolutePath() + "'");
		this.sDirectoryXmlSource = sDirectory;
	}
	
	public String getDirectoryOutput(){
		return this.sDirectoryXsltOutput;
	}
	public void setDirectoryOutput(String sDirectory){
		this.sDirectoryXsltOutput = sDirectory;
	}
}
