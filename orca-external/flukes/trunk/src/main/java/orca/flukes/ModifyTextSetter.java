package orca.flukes;

import orca.flukes.ndl.ModifySaver;
import orca.flukes.ui.TextAreaDialog.ITextSetter;

/**
 * Most likely a temporary class that submits modify
 * request once the text is set
 * @author ibaldin
 *
 */
public class ModifyTextSetter implements ITextSetter {
	String sid = null;
	
	public ModifyTextSetter(String sliceId) {
		sid = sliceId;
	}
	
	@Override
	public void setText(String t) {
		GUIManifestState.getInstance().modifySlice(sid, t);
		ModifySaver.getInstance().clear();
	}

}
