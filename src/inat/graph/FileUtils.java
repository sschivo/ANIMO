package inat.graph;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import cytoscape.Cytoscape;

/**
 * The class used for file input/output
 */
public class FileUtils {
	private static File currentDirectory = null;
	
	/**
	 * Show the File Chooser dialog and return the name of the chosen file.
	 * You can choose the file type (ex. ".csv"), the description (ex. "Commma
	 * separated files"), and the parent Component (null is ok: it is just to
	 * tell the O.S. to which window the dialog will "belong") 
	 * @param fileType The file type (ex. ".png")
	 * @param description The file type description (ex. "Image file")
	 * @param parent The parent Component (typically a window. null is ok)
	 * @return The complete (absoluite) path of the file selected by the user, or
	 * null if the user has selected no file/closed the dialog
	 */
	public static String open(final String fileType, final String description, Component parent) {
		if (currentDirectory == null && Cytoscape.getCurrentSessionFileName() != null) {
			File curSession = new File(Cytoscape.getCurrentSessionFileName());
			if (curSession != null && curSession.exists()) {
				currentDirectory = curSession.getParentFile();
			}
		}
		JFileChooser chooser = new JFileChooser(currentDirectory);
		if (fileType != null) {
			chooser.setFileFilter(new FileFilter() {
				public boolean accept(File pathName) {
					if (pathName.getAbsolutePath().endsWith(fileType) || pathName.isDirectory()) {
						return true;
					}
					return false;
				}
	
				public String getDescription() {
					return description;
				}
			});
		} else {
			chooser.setFileFilter(new FileFilter() {
				public boolean accept(File pathName) {
					return true;
				}
	
				public String getDescription() {
					return description;
				}
			});
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int result = chooser.showOpenDialog(parent);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}
	
	/**
	 * Show the save dialog. The workings are the same as the with the open function
	 * @param fileType The file type (ex. ".png")
	 * @param description The file type description (ex. "Image file")
	 * @param parent The parent Component (typically a window. null is ok)
	 * @return The complete (absoluite) path of the file selected by the user, or
	 * null if the user has selected no file/closed the dialog
	 */
	public static String save(final String fileType, final String description, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File pathName) {
				if (pathName.getAbsolutePath().endsWith(fileType) || pathName.isDirectory()) {
					return true;
				}
				return false;
			}

			public String getDescription() {
				return description;
			}
		});
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int result = chooser.showSaveDialog(parent);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			String fileName = chooser.getSelectedFile().getAbsolutePath();
			if (!fileName.endsWith(fileType)) {
				fileName += fileType;
			}
			return fileName;
		}
		return null;
	}
	
	/**
	 * Save what is currently shown on the given Component to a given .png file
	 * @param c The component whose "photograph" is to be saved
	 * @param fileName The name of the file in which to save the image
	 */
	public static void saveToPNG(Component c, String fileName) {
		try {
			BufferedImage image = new BufferedImage(c.getSize().width, c.getSize().height, BufferedImage.TYPE_INT_RGB);
			Graphics imgGraphics = image.createGraphics();
			c.paint(imgGraphics );
			File f = new File(fileName);
			f.delete();
			ImageIO.write(image, "png", f);
			imgGraphics =null;
			image=null;
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Save what is currently shown on the given Component to a
	 * file that the user will choose via the open dialog.
	 * @param c The component to be saved
	 */
	public static void saveToPNG(Component c) {
		String fileName = save(".png", "PNG image", c);
		if (fileName != null) {
			saveToPNG(c, fileName);
		}
	}
	
	/**
	 * Save what is currently shown on the given Graph to a given .jpg file,
	 * rendering it with given size
	 * @param graph The Graph whose "photograph" is to be saved
	 * @param fileName The name of the file in which to save the image
	 * @param width Width of the resulting image
	 * @param heigth Height of the resulting image
	 */
	public static void renderToJPG(Graph graph, String fileName, int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics imgGraphics = image.createGraphics();
			graph.ensureRedraw();
			graph.paint(imgGraphics, width, height);
			graph.ensureRedraw();
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
			if (!iter.hasNext()) {
				throw new Exception("No writer for JPEG format");
			}
			ImageWriter writer = iter.next();
			ImageWriteParam iwp = writer.getDefaultWriteParam();
			iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			String qualityS = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Compression quality in [0, 1] (0 = worst quality, highest compression; 1 = best quality, lowest compression)", 1);
			if (qualityS == null) return;
			float quality = 1.0f;
			try {
				quality = Float.parseFloat(qualityS);
			} catch (NumberFormatException ex) {
			}
			iwp.setCompressionQuality(quality);
			File f = new File(fileName);
			f.delete();
			FileImageOutputStream output = new FileImageOutputStream(f);
			//ImageIO.write(image, "jpg", f);
			writer.setOutput(output);
			writer.write(image);
			writer.dispose();
			imgGraphics = null;
			image = null;
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Graph exported to " + f.getCanonicalPath() + ".", "Operation successful", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Error: " + e.getMessage() + ", caused by " + e.getCause() + ".", "Operation failed", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Save what is currently shown on the given Component to a
	 * file that the user will choose via the open dialog.
	 * The resulting image will be in JPEG format, and will have
	 * a size that is also specified by the user.
	 * @param graph The component to be saved
	 */
	public static void renderToJPG(Graph graph) {
		String fileName = save(".jpg", "JPEG image", graph);
		if (fileName == null) return;
		String widthS = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Width of the image", graph.getSize().width);
		if (widthS == null) return;
		int width = graph.getSize().width;
		try {
			width = Integer.parseInt(widthS);
		} catch (NumberFormatException ex) {
		}
		String heightS = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Height of the image", graph.getSize().height);
		if (heightS == null) return;
		int height = graph.getSize().height;
		try {
			height = Integer.parseInt(heightS);
		} catch (NumberFormatException ex) {
		}
		String zoomS = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Zoom level", graph.getZoomLevel());
		if (zoomS == null) return;
		int zoom = graph.getZoomLevel();
		try {
			int zoomTmp = Integer.parseInt(zoomS);
			if (zoomTmp > 0) {
				zoom = zoomTmp;
			}
		} catch (NumberFormatException ex) {
		}
		int oldZoom = graph.getZoomLevel();
		graph.setZoomLevel(zoom);
		renderToJPG(graph, fileName, width, height);
		graph.setZoomLevel(oldZoom);
	}
}
