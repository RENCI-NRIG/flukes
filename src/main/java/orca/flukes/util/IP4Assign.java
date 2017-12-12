package orca.flukes.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	private static final int MP_MASK_SIZE = 24;
	private static final String PP_START_ADDRESS="172.16.0.1";
	private static final String MP_START_ADDRESS="172.16.100.1";
	// min/max allowed values
	private static int[] ppBrackets = new int[2], mpBrackets = new int[2];
	
	// for debugging set to true
	private boolean debugOutput = false;
	
	static {
		try {
			ppBrackets[0] = InetAddresses.coerceToInteger(InetAddress.getByName(PP_START_ADDRESS));
			ppBrackets[1] = InetAddresses.coerceToInteger(InetAddress.getByName(MP_START_ADDRESS)) - 2;
			mpBrackets[0] = InetAddresses.coerceToInteger(InetAddress.getByName(MP_START_ADDRESS));
			mpBrackets[1] = mpBrackets[0] + 0x9AFF;
		} catch (UnknownHostException uhe) {
			;
		}
	}
	
	private Inet4Address ppCurrent, mpCurrent;
	private final int ppMaskSize, mpMaskSize;
	private int mpStartInt;
	
	public static class AssignedRange {
		private String startAddress;
		private int qty;
		
		public AssignedRange(String s, int q) {
			startAddress = s;
			qty = q;
		}
		
		public String getStart() {
			return startAddress;
		}
		
		public int getQty() {
			return qty;
		}
	}
	
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
	public String[] getPPAddresses(List<AssignedRange> assignedToGraph) {
		List<String> usedInGraph = new ArrayList<>();
		
		debugPrint("Requesting PP address ");
		
		debugPrint("Assigned addresses: " + assignedToGraph);
		 
		if (assignedToGraph != null) {
			// create explicit list of all assigned addresses
			try {
				for(AssignedRange ar: assignedToGraph) {
					if (ar.getQty() == 1)
						usedInGraph.add(ar.getStart());
					else {
						String start = ar.getStart();
						usedInGraph.add(start);
						InetAddress startAddr = (Inet4Address)InetAddress.getByName(start);
						for(int i = 1; i < ar.getQty(); i++) {
							startAddr = InetAddresses.increment(startAddr);
							usedInGraph.add(startAddr.getHostAddress());
						}
					}
				}
			} catch(UnknownHostException uhe) {
				;
			}
		}
		
		debugPrint("Finding highest used");
		int ppCurrentInt = InetAddresses.coerceToInteger(ppCurrent);
		int highest = ppCurrentInt;
		InetAddress tmp = null;
		try {
			debugPrint("Looking for highest address among " + usedInGraph);
			for(String u: usedInGraph) {
				Inet4Address cur = (Inet4Address)InetAddress.getByName(u);
				int curInt = InetAddresses.coerceToInteger(cur);
				if (curInt > ppBrackets[1])
					continue;
				if (curInt > highest) {
					highest = curInt;
					// debugPrint("   " + u);
					tmp = cur;
				}
			}
		} catch(UnknownHostException uhe) {
			;
		}
		
		debugPrint("Highest " + highest + " current " + ppCurrentInt);
		
		long subnets = Math.round(Math.ceil((double)(highest - ppCurrentInt)/4L));
		debugPrint("Incrementing by " + subnets + " subnets");
		ppCurrentInt += (subnets<<2);
		ppCurrent = InetAddresses.fromInteger(ppCurrentInt);
		tmp = ppCurrent;

		String[] ret = new String[2];
		
		if (InetAddresses.coerceToInteger(ppCurrent) == mpStartInt)
			return null;
		
		ret[0] = ppCurrent.getHostAddress();
		tmp = InetAddresses.increment(ppCurrent);
		ret[1] = tmp.getHostAddress();
		
		ppCurrentInt = InetAddresses.coerceToInteger(ppCurrent);
		ppCurrentInt += 1L << (32 - ppMaskSize);
		
		ppCurrent = InetAddresses.fromInteger(ppCurrentInt);
		
		debugPrint("Issuing " + Arrays.asList(ret));
		
		return ret;
	}
	
	/**
	 * Uses the setting of MP mask to issue. 
	 * Use getMPMask() to get the netmask for mp links 
	 * @param ct - number of addresses needed 
	 * @param assignedToLink - list of assigned ranges representing already occupied addresses in this subnet
	 * @param assignedToGraph - list of assigned ranges already assigned to the entire graph.
	 * @return - array of addresses or null if no addresses are available
	 */
	public String[] getMPAddresses(int ct, List<AssignedRange> assignedToLink, List<AssignedRange> assignedToGraph) {
		assert(ct > 0);
		assert(assignedToGraph != null);

		debugPrint("Requesting " + ct + " MP addresses for netmask " + mpMaskSize);

		List<String> usedOnLink = new ArrayList<>();
		List<String> usedInGraph = new ArrayList<>();
		
		// create an explicit list of assigned addresses
		if (assignedToLink != null) {
			try {
				for(AssignedRange ar: assignedToLink) {
					if (ar.getQty() == 1)
						usedOnLink.add(ar.getStart());
					else {
						String start = ar.getStart();
						usedOnLink.add(start);
						InetAddress startAddr = (Inet4Address)InetAddress.getByName(start);
						for(int i = 1; i < ar.getQty(); i++) {
							startAddr = InetAddresses.increment(startAddr);
							usedOnLink.add(startAddr.getHostAddress());
						}
					}
				}
			} catch(UnknownHostException uhe) {
				;
			}
		} 
		
		if (assignedToGraph != null) {
			// create explicit list of all assigned addresses
			try {
				for(AssignedRange ar: assignedToGraph) {
					if (ar.getQty() == 1)
						usedInGraph.add(ar.getStart());
					else {
						String start = ar.getStart();
						usedInGraph.add(start);
						InetAddress startAddr = (Inet4Address)InetAddress.getByName(start);
						for(int i = 1; i < ar.getQty(); i++) {
							startAddr = InetAddresses.increment(startAddr);
							usedInGraph.add(startAddr.getHostAddress());
						}
					}
				}
			} catch(UnknownHostException uhe) {
				;
			}
		}
			
		debugPrint("Assigned addresses: " + usedOnLink);
		debugPrint("All used addresses: " + usedInGraph);
		
		int usedCnt = ct + usedOnLink.size();
		
		if (usedCnt > 1L << 32 - mpMaskSize)
			return null;
		
		String[] ret = new String[ct];
		
		// if used is empty, start a new subnet, if not, use existing subnet
		InetAddress tmp = null;
		
		if (usedOnLink.size() == 0) {
			debugPrint("Finding highest used");
			int mpCurrentInt = InetAddresses.coerceToInteger(mpCurrent);
			int highest = mpCurrentInt;
			try {
				debugPrint("Looking for highest address among " + usedInGraph);
				for(String u: usedInGraph) {
					Inet4Address cur = (Inet4Address)InetAddress.getByName(u);
					int curInt = InetAddresses.coerceToInteger(cur);
					if (curInt > mpBrackets[1])
						continue;
					if (curInt > highest) {
						highest = curInt;
						// debugPrint("   " + u);
						tmp = cur;
					}
				}
			} catch(UnknownHostException uhe) {
				;
			}
			debugPrint("Highest " + highest + " current " + mpCurrentInt);
			long subnets = Math.round(Math.ceil((double)(highest - mpCurrentInt)/(1L << (32 - mpMaskSize))));
			debugPrint("Incrementing by " + subnets + " subnets");
			mpCurrentInt += subnets*(1L << (32 - mpMaskSize));
			mpCurrent = InetAddresses.fromInteger(mpCurrentInt);
			tmp = mpCurrent;
		} else {
			// find the smallest assigned address
			//int smallest = mpBrackets[1];
			try {
				Inet4Address first = (Inet4Address)InetAddress.getByName(usedOnLink.get(0));
				int smallest = InetAddresses.coerceToInteger(first);
				debugPrint("Looking for smallest address among " + usedOnLink);
				for(String u: usedOnLink) {
					Inet4Address cur = (Inet4Address)InetAddress.getByName(u);
					int curInt = InetAddresses.coerceToInteger(cur);
					if (curInt < mpBrackets[0])
						continue;
					if (curInt < smallest) {
						smallest = curInt;
					}
				}
				debugPrint("Lowest " + smallest);
				tmp = InetAddresses.fromInteger(smallest);
			} catch(UnknownHostException uhe) {
				;
			}
		}
		debugPrint("Starting from " + tmp);
		
		for(int i = 0; ct > 0; ct--, i++) {
			String candidate = null;
			while(true) {
				candidate = tmp.getHostAddress();
				// skip already assigned addresses
				debugPrint("Checking candidate " + candidate + " against " + usedOnLink);
				boolean found = false;
				for(String u: usedOnLink) {
					if (u.equals(candidate)) {
						tmp = InetAddresses.increment(tmp);
						found = true;
						break;
					}
				}
				if (!found)
					break;
			}
			debugPrint("Assigning candidate " + candidate);
			ret[i] = candidate;
			tmp = InetAddresses.increment(tmp);
		}
		
		int mpCurrentInt = InetAddresses.coerceToInteger(mpCurrent);
		mpCurrentInt += 1L << (32 - mpMaskSize);
		mpCurrent = InetAddresses.fromInteger(mpCurrentInt);
		debugPrint("Issuing " + Arrays.asList(ret));
		return ret;
	}
	
	private void debugPrint(String s) {
		if (debugOutput)
			System.out.println(s);
	}
	
	public static void main(String[] argv) {
		IP4Assign ipa = new IP4Assign();
		
		System.out.println(ipa.getMPMask());
		System.out.println(ipa.getPPMask());
		
		for (int i = 0; i < 200; i++ ) {
			String[] ret = ipa.getPPAddresses(null);
			if (ret != null)
				System.out.println(ret[0] + " " + ret[1]);
			else
				break;
		}
		
		for(int i = 0; i < 10; i++) {
			String [] ret = ipa.getMPAddresses(8, null, new ArrayList<AssignedRange>());
			if (ret != null) {
				for (int j = 0; j < 8; j++)
					System.out.println(ret[j]);
				System.out.println("------");
			}
		}
		
	}
}
