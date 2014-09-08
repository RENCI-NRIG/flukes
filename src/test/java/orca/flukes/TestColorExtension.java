package orca.flukes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import orca.flukes.ndl.ManifestLoader;
import orca.flukes.ndl.RequestLoader;

import org.junit.Test;

// load a sample file with color extension
public class TestColorExtension {
	

public void readColorRequest() throws IOException {
	System.setProperty("java.awt.headless", "true");
	InputStream is = this.getClass().getResourceAsStream("/test-color-extension.rdf");
	assert(is != null);
	String t = new Scanner(is).useDelimiter("\\A").next();
	RequestLoader rl = new RequestLoader();
	rl.loadGraph(t);
}


public void readColorManifest() throws IOException {
	System.setProperty("java.awt.headless", "true");
	InputStream is = this.getClass().getResourceAsStream("/test-color-extension-manifest.rdf");
	assert(is != null);
	String t = new Scanner(is).useDelimiter("\\A").next();
	ManifestLoader ml = new ManifestLoader();
	ml.loadString(t);
}
	
}
