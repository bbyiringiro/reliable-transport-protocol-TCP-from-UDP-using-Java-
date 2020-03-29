
// 
/**
 * The Class Common.
 * Is a Utility class that contains static functions that will be used by various classes
 */
final public class Common {
	
	/**
	 * Check argument length passed vs the one required by a particular program.
	 *
	 * @param givenLen the given length of arguments
	 * @param requiredsize the requiredsize of number of arguments
	 * @throws IllegalArgumentException the illegal argument exception if the number doesn't match
	 */
	public static void checkArgumentLength(int givenLen, int requiredsize) throws IllegalArgumentException{
		if(givenLen != requiredsize) {
			throw new IllegalArgumentException("Few or incorrect Arguments");
		}
	}
	
	
	/**
	 * Simplified java string print function
	 *
	 * @param s the string
	 */
	public static void print(String s) {
		System.out.println(s);
	}
}
