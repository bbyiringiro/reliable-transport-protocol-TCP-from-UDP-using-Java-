
public class Common {
	public static void checkArgumentLength(int givenLen, int requiredsize) throws IllegalArgumentException{
		if(givenLen != requiredsize) {
			throw new IllegalArgumentException("Few or incorrect Arguments");
		}
	}
}
