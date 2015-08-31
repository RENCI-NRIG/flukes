package orca.flukes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orca.flukes.GUI.PrefsEnum;
import orca.flukes.xmlrpc.RegistryXMLRPCProxy;

import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

/**
 * Manage the list of known images
 * @author ibaldin
 *
 */
public class GUIImageList {
	static final String IMAGE_NAME_SUFFIX = "-req";
	public static final String NO_GLOBAL_IMAGE = "None";
	// VM images defined by the user
	TreeMap<String, OrcaImage> definedImages;
	
	// are we adding a new image definition or editing existing
	boolean addingNewImage = false;

	private static GUIImageList instance = new GUIImageList();
	
	public static GUIImageList getInstance() {
		return instance;
	}
	
	private GUIImageList() {
		definedImages = new TreeMap<>();
	}
	
	public OrcaImage getImageByName(String nm) {
		return definedImages.get(nm);
	}
	
	public String addImage(OrcaImage newIm, OrcaImage oldIm) {
		if (newIm == null)
			return null;
		
		String retImageName = newIm.getShortName();
		
		// if old image is not null, then we are replacing, so delete first
		if (oldIm != null) {
			definedImages.remove(oldIm.getShortName());
		} else {
			// if old image is null, we should check if there is already an image
			// with that name and if its URL and hash match. If not ???
			oldIm = definedImages.get(newIm.getShortName());
			if (oldIm != null) {
				if (!oldIm.getHash().equals(newIm.getHash()) || !oldIm.getUrl().equals(newIm.getUrl())) {
					// try to find a new name, substitute it for the old one
					if (definedImages.containsKey(retImageName + GUIImageList.IMAGE_NAME_SUFFIX)) {
						int i = 1;
						for(;definedImages.containsKey(retImageName + GUIImageList.IMAGE_NAME_SUFFIX + i);i++);
						retImageName += GUIImageList.IMAGE_NAME_SUFFIX + i;
					} else
						retImageName += GUIImageList.IMAGE_NAME_SUFFIX;
					newIm.substituteName(retImageName);
					definedImages.put(retImageName, newIm);
				} else {
					// nothing to do - same image
				}
			} 
		}
		
		definedImages.put(retImageName, newIm);
		return retImageName;
	}
	
	/**
	 * Add images from a list (of preferences)
	 * @param newIm
	 */
	public void addImages(List<OrcaImage> newIm) {
		for (OrcaImage im: newIm) {
			addImage(im, null);
		}
	}
	
	public Object[] getImageShortNames() {
		if (definedImages.size() > 0)
			return definedImages.keySet().toArray();
		else return new String[0];
	}
	
	public String[] getImageShortNamesWithNone() {
		String[] fa = new String[definedImages.size() + 1];
		fa[0] = GUIImageList.NO_GLOBAL_IMAGE;
		System.arraycopy(getImageShortNames(), 0, fa, 1, definedImages.size());
		return fa;		
	}
	
	public Iterator<String> getImageShortNamesIterator() {
		return definedImages.keySet().iterator();
	}
	
	public void clear() {
		addingNewImage = false;
		definedImages.clear();
	}
	
	/**
	 * Return null if 'None' image is asked for
	 * @param n
	 * @param image
	 */
	public static String getNodeImageProper(String image) {
		if ((image == null) || image.equals(GUIImageList.NO_GLOBAL_IMAGE))
			return null;
		else
			return image;
	}
	
	/**
	 * Get images from preferences imageX.name imageX.url and imageX.hash
	 * @return list of OrcaImage beans
	 */
	void getImagesFromPreferences() {
		List<OrcaImage> images = new ArrayList<OrcaImage>();
		
		// add the default
		try {
			images.add(new OrcaImage(GUI.getInstance().getPreference(PrefsEnum.IMAGE_NAME), 
					new URL(GUI.getInstance().getPreference(PrefsEnum.IMAGE_URL)), GUI.getInstance().getPreference(PrefsEnum.IMAGE_HASH)));
		} catch (MalformedURLException ue) {
			;
		}
		
		// see if there are more
		int i = 1;
		while(true) {
			String nmProp = "image" + i + ".name";
			String urlProp = "image" + i + ".url";
			String hashProp = "image" + i + ".hash";
			
			String nmPropVal = GUI.getInstance().getPreference(nmProp);
			String urlPropVal = GUI.getInstance().getPreference(urlProp);
			String hashPropVal = GUI.getInstance().getPreference(hashProp);
			if ((nmPropVal != null) && (urlPropVal != null) && (hashPropVal != null)) {
				try {
					if ((nmPropVal.trim().length() > 0) && (urlPropVal.trim().length() > 0) && (hashPropVal.trim().length() > 0))
						images.add(new OrcaImage(nmPropVal.trim(), new URL(urlPropVal.trim()), hashPropVal.trim()));
				} catch (MalformedURLException ue) {
					;
				}
			} else
				break;
			i++;
		}
		
		addImages(images);
	}
	
	/**
	 * Get images from image registry
	 */
	void getImagesFromRegistry() {
		List<OrcaImage> images = new ArrayList<OrcaImage>();
		
		List<Map<String, String>> regImages = null;
		try {
			regImages = RegistryXMLRPCProxy.getInstance().getImages();
		} catch (Exception e) {
			KMessageDialog md = new KMessageDialog(GUI.getInstance().getFrame(), "Unable to fetch images from image registry.", true);
			md.setMessage("Unable to fetch images from image registry  " + PrefsEnum.ORCA_REGISTRY + ". This is not a fatal error.");
			md.setLocationRelativeTo(GUI.getInstance().getFrame());
			md.setVisible(true);
			return;
		}
		
		for (Map<String, String> regImg: regImages) {
			if ((regImg.get("ImageName") != null) &&
					(regImg.get("ImageURL") != null) && 
					(regImg.get("ImageHash") != null)) {
				try {
					OrcaImage im = new OrcaImage(regImg.get("ImageName"), 
							new URL(regImg.get("ImageURL")), 
							regImg.get("ImageHash"));
					images.add(im);
				} catch (MalformedURLException me) {
					continue;
				}
			}
		}
		addImages(images);
	}
	
	/**
	 * Collect all images from preferences and image registry
	 */
	void collectAllKnownImages() {
		clear();
		getImagesFromPreferences();
		getImagesFromRegistry();
	}
	
}
