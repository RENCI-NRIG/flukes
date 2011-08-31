package orca.flukes.ui;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hyperrealm.kiwi.ui.DataField;

/**
 * Parse time in a simple format HH:MM 
 * @author ibaldin
 *
 */
@SuppressWarnings("serial")
public class TimeField extends DataField<String> {
	private static String[] timePatterns = { 
		"[0-2][0-9]:[0-6][0-9]",
		"[0-9]:[0-6][0-9]" };

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
