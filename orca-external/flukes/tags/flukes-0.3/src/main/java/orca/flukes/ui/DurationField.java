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

import javax.swing.JLabel;

import com.hyperrealm.kiwi.text.FormatConstants;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.NumericField;

/**
 * Specify slice request duration in days, hours, minutes
 * @author ibaldin
 *
 */
public class DurationField extends KPanel {
	NumericField days, hours, minutes;
	
	public void setDurationField(int d, int h, int m) {
		if (d > 0)
			days.setValue(d);
		if (h > 0)
			hours.setValue(h);
		if (m > 0)
			minutes.setValue(m);
	}

	public int getDays() {
		return (int)days.getValue();
	}
	
	public int getHours() {
		return (int)hours.getValue();
	}

	public int getMinutes() {
		return (int)minutes.getValue();
	}
	
	public DurationField() {
		JLabel lblNewLabel = new JLabel("Days:");
		add(lblNewLabel);

		days = new NumericField(3);
		days.setMinValue(0);
		days.setType(FormatConstants.INTEGER_FORMAT);
		days.setDecimals(0);
		add(days);

		lblNewLabel = new JLabel("Hrs:");
		add(lblNewLabel);

		hours = new NumericField(3);
		hours.setMinValue(0);
		hours.setType(FormatConstants.INTEGER_FORMAT);
		hours.setDecimals(0);
		add(hours);

		lblNewLabel = new JLabel("Min:");
		add(lblNewLabel);
		minutes = new NumericField(3);
		minutes.setMinValue(0);
		minutes.setType(FormatConstants.INTEGER_FORMAT);
		minutes.setDecimals(0);
		add(minutes);
	}
}
