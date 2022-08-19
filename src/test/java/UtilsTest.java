import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {


    @Test
    public void testStripLeadingZeros_anynumber() throws Exception {
        String result = Utils.stripLeadingZeros("002");
        Assert.assertEquals("2", result);
    }

    @Test
    public void testStripLeadingZeros_zero() throws Exception {
        String result = Utils.stripLeadingZeros("0000000");
        Assert.assertEquals("0", result);
    }
}