package orca.flukes.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import orca.flukes.ndl.RequestSaver;

import com.google.common.net.InetAddresses;

/**
 * Class to manage ip address assignments
 * @author ibaldin
 *
 */
public class IP4Assign {
	public static InetAddress start;
	private static final int PP_MASK_SIZE = 30;
	private static final int MP_MASK_SIZE = 25;
	private static final String PP_START_ADDRESS="172.16.0.1";
	private static final String MP_START_ADDRESS="172.16.100.1";
	
	private Inet4Address ppCurrent, mpCurrent;
	private final int ppMaskSize, mpMaskSize;
	private int mpStartInt;
	
	/**
	 * Use netmask of specified length for multipoint links
	 * @param mpSz
	 */
	public IP4Assign(int mpSz) {

		ppMaskSize = PP_MASK_SIZE;
		mpMaskSize = mpSz;
		init();
	}
	
	/**
	 * Use default mask length for multipoint links
	 */
	public IP4Assign() {

		ppMaskSize = PP_MASK_SIZE;
		mpMaskSize = MP_MASK_SIZE;
		init();
	}
	
	private void init() {
		try {
			ppCurrent = (Inet4Address)InetAddress.getByName(PP_START_ADDRESS);
			mpCurrent = (Inet4Address)InetAddress.getByName(MP_START_ADDRESS);
			mpStartInt = InetAddresses.coerceToInteger(mpCurrent);
		} catch (UnknownHostException e) {
			;
		}
	}
	
	/**
	 * Get the p-to-p mask
	 * @return
	 */
	public String getPPMask() {
		return RequestSaver.netmaskIntToString(ppMaskSize);
	}
	
	public int getPPIntMask() {
		return ppMaskSize;
	}
	
	/**
	 * Get the mp mask
	 * @return
	 */
	public String getMPMask() {
		return RequestSaver.netmaskIntToString(mpMaskSize);
	}

	public int getMPIntMask() {
		return mpMaskSize;
	}
	
	/**
	 * Issues two unique addresses for p-to-p link
	 * Use getPPmask() to get the netmask for p-to-p links
	 * @return - array of 2 elements with addresses or null if no addresses are available
	 */
	public String[] getPPAddresses() {
		String[] ret = new String[2];
		
		if (InetAddresses.coerceToInteger(ppCurrent) == mpStartInt)
			return null;
		
		ret[0] = ppCurrent.getHostAddress();
		InetAddress tmp = InetAddresses.increment(ppCurrent);
		ret[1] = tmp.getHostAddress();
		
		int ppCurrentInt = InetAddresses.coerceToInteger(ppCurrent);
		ppCurrentInt += 1L << (32 - ppMaskSize);
		
		ppCurrent = InetAddresses.fromInteger(ppCurrentInt);
		
		return ret;
	}
	
	/**
	 * Uses the setting of MP mask to issue. 
	 * Use getMPMask() to get the netmask for mp links 
	 * @param ct - number of addresses needed 
	 * @return - array of addresses or null if no addresses are available
	 */
	public String[] getMPAddresses(int ct) {
		assert(ct > 0);

		if (ct > 1L << 32 - mpMaskSize)
			return null;
		
		String[] ret = new String[ct];
		
		InetAddress tmp = mpCurrent;
		for(int i = 0; ct > 0; ct--, i++) {
			ret[i] = tmp.getHostAddress();
			tmp = InetAddresses.increment(tmp);
		}
		
		int mpCurrentInt = InetAddresses.coerceToInteger(mpCurrent);
		mpCurrentInt += 1L << (32 - mpMaskSize);
		mpCurrent = InetAddresses.fromInteger(mpCurrentInt);
		
		return ret;
	}
	
	public static void main(String[] argv) {
		IP4Assign ipa = new IP4Assign();
		
		System.out.println(ipa.getMPMask());
		System.out.println(ipa.getPPMask());
		
		for (int i = 0; i < 200; i++ ) {
			String[] ret = ipa.getPPAddresses();
			if (ret != null)
				System.out.println(ret[0] + " " + ret[1]);
			else
				break;
		}
		
		for(int i = 0; i < 10; i++) {
			String [] ret = ipa.getMPAddresses(8);
			if (ret != null) {
				for (int j = 0; j < 8; j++)
					System.out.println(ret[j]);
				System.out.println("------");
			}
		}
		
	}
}
