/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
package orca.flukes.ui;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KLabel;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.NumericField;

public class IpAddrField extends KPanel {
	private NumericField o1;
	private NumericField o2;
	private NumericField o3;
	private NumericField o4;
	private NumericField nm;
	private String ipPattern = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
	
	public String getAddress() {
		// 0 in first octet is meaningless
		if ((int)o1.getValue() == 0)
			return null;
		return "" + (int)o1.getValue() + "." + (int)o2.getValue() + 
		"." + (int)o3.getValue() + "." + (int)o4.getValue();
	}
	
	public String getNetmask() {
		if (((int)nm.getValue() < 1) || ((int)nm.getValue() > 32))
			return null;
		return "" + (int)nm.getValue();
	}
	
	public void setAddress(String s, String maskString) {
		if (s == null)
			return;
		if (!s.matches(ipPattern))
			return;
		String[] octets = s.split("\\.");
		o1.setValue(Integer.parseInt(octets[0]));
		o2.setValue(Integer.parseInt(octets[1]));
		o3.setValue(Integer.parseInt(octets[2]));
		o4.setValue(Integer.parseInt(octets[3]));
		if (maskString != null) {
			int mask = Integer.parseInt(maskString);
			if ((mask >= 1) && (mask <= 32))
				nm.setValue(mask);
		} 
	}
	
	/**
	 * Create the panel. Fixed mask true means don't allow changing the mask
	 */
	public IpAddrField() {
		
		o1 = new NumericField(3);
		o1.setMinValue(1);
		o1.setMaxValue(255);
		o1.setType(FormatConstants.INTEGER_FORMAT);
		o1.setDecimals(0);
		add(o1);
		o1.setColumns(3);
		
		KLabel dot1 = new KLabel(".");
		add(dot1);
		
		o2 = new NumericField(3);
		o2.setMinValue(0);
		o2.setMaxValue(255);
		o2.setType(FormatConstants.INTEGER_FORMAT);
		o2.setDecimals(0);
		add(o2);
		o2.setColumns(3);
		
		KLabel dot2 = new KLabel(".");
		add(dot2);
		
		o3 = new NumericField(3);
		o3.setMinValue(0);
		o3.setMaxValue(255);
		o3.setType(FormatConstants.INTEGER_FORMAT);
		o3.setDecimals(0);
		add(o3);
		o3.setColumns(3);
		
		KLabel dot3 = new KLabel(".");
		add(dot3);
		
		o4 = new NumericField(3);
		o4.setMinValue(0);
		o4.setMaxValue(255);
		o4.setType(FormatConstants.INTEGER_FORMAT);
		o4.setDecimals(0);
		add(o4);
		o4.setColumns(3);
		
		KLabel sl = new KLabel("/");
		add(sl);
			
		nm = new NumericField(2);
		nm.setMinValue(1);
		nm.setMaxValue(32);
		nm.setType(FormatConstants.INTEGER_FORMAT);
		nm.setDecimals(0);
		add(nm);
		nm.setColumns(2);
	}

}
