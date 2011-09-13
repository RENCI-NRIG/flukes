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

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hyperrealm.kiwi.ui.DataField;

/**
 * Parse time in a simple format HH:MM (24hr system)
 * @author ibaldin
 *
 */
@SuppressWarnings("serial")
public class TimeField extends DataField<String> {
	private static String[] timePatterns = { 
		"[0-2][0-9]:[0-5][0-9]",
		"[0-9]:[0-5][0-9]" };

	public TimeField() {
		super(5);
	}

	private boolean checkTimeString(String tS) {
		for (String pat: timePatterns) {
			if (tS.matches(pat)) { 
				int hr = Integer.parseInt(tS.split(":")[0]);
				int min = Integer.parseInt(tS.split(":")[1]);
				if ((hr <= 23) && (min <=59))
					return true;
				return false;
			}
		}
		return false;
	}

	@Override
	protected boolean checkInput() {
		boolean t = checkTimeString(getText());
		paintInvalid(!t);
		return t;
	}

	public void setTime(String s) {
		setText(s);
		paintInvalid(!checkInput());
	}

	public String getTime() {
		if (checkInput())
			return getText();
		else return null;
	}

	/**
	 * get hour within day
	 * @return
	 */
	public int getHour() {
		String t = getTime();
		
		if (t == null)
			return -1;
		
		return Integer.parseInt(t.split(":")[0]);
	}
	
	/**
	 * get minutes within hour
	 * @return
	 */
	public int getMinute() {
		String t = getTime();
		
		if (t == null)
			return -1;
		
		return Integer.parseInt(t.split(":")[1]);
	}
	
	/**
	 * get minutes within day
	 */
	public int getMinutes() {
		int hour = getHour();

		if (hour == -1 )
			return -1;
		
		return hour*60 + getMinute();
	}
	
	public void setTime(int h, int m) {
		setTime(String.format("%d:%02d", h, m));
	}
	
	/**
	 * Set field to current time
	 */
	public void setNow() {
		
		setTime(String.format("%d:%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY), 
				Calendar.getInstance().get(Calendar.MINUTE)));
	}
	
	@Override
	public String getObject() {
		return getTime();
	}

	@Override
	public void setObject(String o) {
		setTime(o);
	}

}
