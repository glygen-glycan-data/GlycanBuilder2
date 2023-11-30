
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.RuntimeException;
import java.lang.IllegalArgumentException;
import java.lang.StringIndexOutOfBoundsException;
import java.lang.NullPointerException;
import java.lang.StackOverflowError;
import java.lang.Error;
import java.lang.StringBuilder;
import java.util.*;

import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.GlycanFormatconverter.Glycan.GlycanException;
import org.glycoinfo.WURCSFramework.util.oldUtil.ConverterExchangeException;
import org.glycoinfo.WURCSFramework.util.array.WURCSFormatException;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTCondensedParser;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.linkage.Union;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.resourcesdb.monosaccharide.MonosaccharideException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;

import java.security.MessageDigest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*; 
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class GlycanImageCmdline
{

	private static String readFileAsString(String filePath) throws IOException{
		byte[] buffer;
		int maxbuf = 10*1024; // 10K, big enough?
		if (filePath.equals("-")) {
			buffer = new byte[maxbuf];
		} else {
			buffer = new byte[(int) new File(filePath).length()];
		}
		InputStream f = null;
		int readlen = 0;
		try {
			if (filePath.equals("-")) {
				f = System.in;
			} else {
				f = new BufferedInputStream(new FileInputStream(filePath));
			}
			readlen = f.read(buffer);
		} finally {
			if (f != null) try { f.close(); } catch (IOException ignored) { }
		}
		if (readlen >= maxbuf) {
			throw new IOException("GlycoCTCondensed glycan on standard input too big!");
		}
		return new String(buffer,0,readlen);
	}

	private static String removeExtn(String filePath) throws IOException {
		return changeExtn(filePath,null,null,true);
	}

	private static String changeExtn(String filePath, String extn) throws IOException {
		return changeExtn(filePath,extn,null,true);
	}

	private static String changeExtn(String filePath, String extn, String old) throws IOException {
		return changeExtn(filePath,extn,old,true);
	}

	private static String changeExtn(String filePath, String newextn,
									 String oldextn, Boolean stripPath) throws IOException {
		if (filePath.equals("-")) {
			throw new IOException("Can't change extension for standard input");
		}
		File f = new File(filePath);
		String name;
		if (stripPath) {
			name = f.getName();
		} else {
			name = f.getPath();
		}
		String extn="";
		String base=name;
		int idx = name.lastIndexOf('.');
		if ((idx >= 1) && (idx <= (name.length()-2))) {
			base = name.substring(0,idx);
			extn = name.substring(idx+1);
		}
		if ((oldextn != null) && (extn != oldextn)) {
			throw new IOException("Bad extension");
		}
                if (newextn == null) {
                    return base;
                } 
		return base + "." + newextn;
	}

	private static void setOrientation(BuilderWorkspace t_gwb, String orientation) {
		if (orientation.equals("RL")) {
			t_gwb.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
		} else if (orientation.equals("LR")) {
			t_gwb.getGraphicOptions().ORIENTATION = GraphicOptions.LR;
		} else if (orientation.equals("BT")) {
			t_gwb.getGraphicOptions().ORIENTATION = GraphicOptions.BT;
		} else if (orientation.equals("TB")) {
			t_gwb.getGraphicOptions().ORIENTATION = GraphicOptions.TB;
		} else {
			throw new IllegalArgumentException("Bad orientation option");
		}
	}

	private static void setDisplay(BuilderWorkspace t_gwb, String display) {
		if (display.equals("normalinfo")) {
			t_gwb.setDisplay(GraphicOptions.DISPLAY_NORMALINFO);
		} else if (display.equals("normal")) {
			t_gwb.setDisplay(GraphicOptions.DISPLAY_NORMAL);
		} else if (display.equals("compact")) {
			t_gwb.setDisplay(GraphicOptions.DISPLAY_COMPACT);
		} else {
			throw new IllegalArgumentException("Bad display option");
		}
	}

	private static void setNotation(BuilderWorkspace t_gwb, String notation) {
		if (notation.equals("cfg")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_CFG);
		} else if (notation.equals("snfg")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_SNFG);
		} else if (notation.equals("cfgbw")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_CFGBW);
		} else if (notation.equals("cfglink")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_CFGLINK);
		} else if (notation.equals("uoxf")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_UOXF);
		} else if (notation.equals("text")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_TEXT);
		} else if (notation.equals("uoxfcol")) {
			t_gwb.setNotation(GraphicOptions.NOTATION_UOXFCOL);
		} else {
			throw new IllegalArgumentException("Bad notation option");
		}
	}

        // from: http://www.java2s.com/example/java-utility-method/xml-nodelist/iterable-nodelist-nodelist-f1617.html
        public static Iterable<Element> iterable(NodeList nodeList) {
	    return () -> new Iterator<Element>() {
		private int index = 0;
		
		@Override
		    public boolean hasNext() {
		    return index < nodeList.getLength();
		}
		
		@Override
		    public Element next() {
		    return ((Element)nodeList.item(index++));
		}
	    };
	}

        public static void changestyle(Element elt, String key, String value) {
	    ArrayList<String> newstyles = new ArrayList<String>();
	    for (String style : elt.getAttribute("style").split(";\\s*")) {
		if (style.startsWith(key+":")) {
		    style = key+":"+value;
		}
		newstyles.add(style);
	    }
	    elt.setAttribute("style",String.join("; ",newstyles)+";");
	}

        public static void setwidthandheight(Element svgelt) {
            String vb = svgelt.getAttribute("viewBox");
            String[] dims = vb.split("\\s+");
            svgelt.setAttribute("width",dims[2]);
            svgelt.setAttribute("height",dims[3]);
        }

        public static String md5hash(String data) throws java.security.NoSuchAlgorithmException {
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    md.update(data.getBytes());
            byte[] digest = md.digest();      
	    StringBuffer hexString = new StringBuffer();
            for (int i = 0;i<digest.length;i++) {
		hexString.append(Integer.toHexString(0xFF & digest[i]));
	    }
	    return hexString.toString(); 
	}

	public static void main(String[] args) throws Exception
	{
                System.setProperty("java.awt.headless", "true");
       
		// GlycanWorkspace -> BuilderWorkspace: different constructor
		GlycanRendererAWT t_grawt = new GlycanRendererAWT();
		BuilderWorkspace t_gwb = new BuilderWorkspace(t_grawt);

		GlycoCTCondensedParser parser = new GlycoCTCondensedParser(true);
		WURCS2Parser wparser = new WURCS2Parser();

		MassOptions mo = new MassOptions();
		boolean mass_opts=false;

		setNotation(t_gwb,"cfg");
		setDisplay(t_gwb,"normalinfo");
		setOrientation(t_gwb,"RL");
		String imagefmt = "png";
		double scale=4.0;
		boolean reducing_end=true;
		boolean opaque=true;
		boolean force=false;
		boolean excep=false;
		String outDir = "";
		String outFile = "";
                String idprefix = "";
                boolean idprefix_from_filename = false;

		for (int i=0; i<args.length; i+=1) {

			if (args[i].equals("format") && args.length > (i+1)) {
				imagefmt = args[i+1];
				i += 1;
				continue;
			}
			if (args[i].equals("scale") && args.length > (i+1)) {
				scale = Double.parseDouble(args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("redend") && args.length > (i+1)) {
				reducing_end = Boolean.parseBoolean(args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("orient") && args.length > (i+1)) {
				setOrientation(t_gwb,args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("notation") && args.length > (i+1)) {
				setNotation(t_gwb,args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("display") && args.length > (i+1)) {
				setDisplay(t_gwb,args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("idprefix") && args.length > (i+1)) {
				idprefix = args[i+1];
				i += 1;
				continue;
			}
			if (args[i].equals("opaque") && args.length > (i+1)) {
				opaque = Boolean.parseBoolean(args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("idprefix_from_filename") && args.length > (i+1)) {
				idprefix_from_filename = Boolean.parseBoolean(args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("force") && args.length > (i+1)) {
				force = Boolean.parseBoolean(args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("errorexit") && args.length > (i+1)) {
				excep = Boolean.parseBoolean(args[i+1]);
				i += 1;
				continue;
			}
			if (args[i].equals("outdir") && args.length > (i+1)) {
				outDir = args[i+1];
				i += 1;
				continue;
			}
			if (args[i].equals("out") && args.length > (i+1)) {
				outFile = args[i+1];
				i += 1;
				continue;
			}

			String glycanstr = readFileAsString(args[i]);
                        if (idprefix_from_filename) {
                            idprefix = removeExtn(args[i]);
                        }
                        
			if (outFile.equals("")) {
			    if (outDir.equals("")) {
				outFile = changeExtn(args[i],imagefmt);
			    } else {
                                File f = new File(args[i]);
			        String name = f.getName();
			        String newname = changeExtn(name,imagefmt);
				outFile = outDir + File.separator + newname;
			    }
			}

			try {
                            File outputfile = new File(outFile);
			    if (force || !outputfile.exists()) {

			    Glycan glycan;
			    if (glycanstr.startsWith("WURCS")) {
				try {
			            glycan = wparser.readGlycan(glycanstr, mo);
				} catch (Exception ex) {
				    throw new GlycanException(ex.getMessage());
				}
			    } else if (glycanstr.startsWith("RES")) {
				try {
			            glycan = parser.readGlycan(glycanstr, mo);
				} catch (Exception ex) {
				    throw new GlycanException(ex.getMessage());
				}
			    } else {
			        throw new IllegalArgumentException("Bad glycan descriptor!");
			    }

			    if (imagefmt.equalsIgnoreCase("png") || imagefmt.equalsIgnoreCase("jpg") || imagefmt.equalsIgnoreCase("jpeg")) {
			        BufferedImage img = t_gwb.getGlycanRenderer().getImage(glycan, opaque, mass_opts, reducing_end, scale);
                                ImageIO.write(img, imagefmt, outputfile);
				System.out.println(args[i]+" -> "+outputfile);
			    }
			    else if (imagefmt.equalsIgnoreCase("svg")) {

                    String t_svg = SVGUtils.getVectorGraphics(t_grawt, new Union<Glycan>(glycan), mass_opts, reducing_end);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

                    dbFactory.setValidating(false);
                    dbFactory.setNamespaceAware(true);
                    dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
                    dbFactory.setFeature("http://xml.org/sax/features/validation", false);
                    dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                    dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    StringBuilder xmlStringBuilder = new StringBuilder(t_svg);
                    ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
                    Document doc = dBuilder.parse(input);
                    // doc.getDocumentElement().normalize();

		    Map<String, String> cpmap = new HashMap<String, String>();

                    Element root = doc.getDocumentElement();
		    changestyle(root, "font-size", "11pt");
		    changestyle(root, "font-family", "ariel, sans-serif");
                    setwidthandheight(root);
		    for (Element gr : iterable(root.getElementsByTagName("g"))) {
			for (Element def : iterable(gr.getElementsByTagName("defs"))) {
			    for (Element cp : iterable(def.getElementsByTagName("clipPath"))) {
				String cpid = cp.getAttribute("id");
				String newcpid = null;
				if (idprefix.equals("")) {
                                    Element pathelt = ((Element)cp.getFirstChild());
				    String d = pathelt.getAttribute("d");
                                    if (pathelt.hasAttribute("style")) {
                                        String style = pathelt.getAttribute("style");
				        newcpid = cpid+":"+md5hash(d+":"+style);
                                    } else {
				        newcpid = cpid+":"+md5hash(d);
                                    }
				} else {
				    newcpid = idprefix+":"+cpid;
				}
				cp.setAttribute("id",newcpid);
				cpmap.put(cpid,newcpid);
			    }
			}
			for (Element gr1 : iterable(gr.getElementsByTagName("g"))) {
			    if (gr1.getAttribute("ID").startsWith("r-1:")) {
				for (Element shape : iterable(gr1.getChildNodes())) {
				    if (shape.hasAttribute("style")) {
					ArrayList<String> newstyles = new ArrayList<String>();
					for (String style : shape.getAttribute("style").split(";\\s*")) {
					    if (style.startsWith("clip-path:url(")) {
						String[] data = style.split("[:(#)]");
						String cpid = data[3];
						style = "clip-path:url(#" + cpmap.get(cpid) + ")";
					    }
					    newstyles.add(style);
					}
					shape.setAttribute("style",String.join("; ",newstyles)+";");
				    }
				}
			    }			   
			    if (!idprefix.equals("") && gr1.hasAttribute("ID")) {
				String ID = gr1.getAttribute("ID");
				ID = idprefix+":"+ID;
				gr1.setAttribute("ID",ID);
			    }
			    changestyle(gr1,"font-family","ariel, sans-serif");
			    if (gr1.getAttribute("data.type").equals("Substituent")) {
				changestyle(gr1,"font-size","11pt");
			    }
			}
		    }

		    TransformerFactory transformerFactory = TransformerFactory.newInstance();
		    Transformer transformer = transformerFactory.newTransformer();
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		    DOMSource sourcedoc = new DOMSource(doc);
		    
                    FileWriter outputfilewriter = new FileWriter(outFile);
		    StreamResult outputfilestream = new StreamResult(outputfilewriter);
		    
		    transformer.transform(sourcedoc, outputfilestream);

                    outputfilewriter.close();

		    System.out.println(args[i]+" -> "+outputfile);
		
	    }
			    else {
			        throw new IllegalArgumentException("Image format " + imagefmt + " is not supported");
			    }
			    }

			}
			catch (GlycanException ex) {
				System.out.println(args[i] + ": " + ex.getClass().getSimpleName() + "-" + ex.getMessage());
                                if (excep) {
                                  throw ex;
                                }
			}
			catch (Exception ex) {
				System.out.println(args[i] + ": " + ex.getClass().getSimpleName());
                                if (excep) {
                                  throw ex;
                                }
			}
			catch (java.lang.Error ex) {
				System.out.println(args[i] + ": " + ex.getClass().getSimpleName());
                                if (excep) {
                                  throw ex;
                                }
			}

			outFile = "";

		}
	}
}
