import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListRemoteZipTest {

    @Test
    void testParseFileList_extractsOnlyFiles() throws IOException {
        byte[] fileName = "file.zip".getBytes(StandardCharsets.UTF_8);
        byte[] dirName = "test/".getBytes(StandardCharsets.UTF_8);
        int totalSize = 2 * 46 + fileName.length + dirName.length;

        ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(0x02014b50); // signature
        buffer.position(28); // nameLength offset
        buffer.putShort((short) fileName.length); // nameLength
        buffer.putShort((short) 0); // extraLength
        buffer.putShort((short) 0); // commentLength
        buffer.position(46); // start of file name
        buffer.put(fileName);

        buffer.putInt(0x02014b50);
        buffer.position(buffer.position() + 24);
        buffer.putShort((short) dirName.length);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.position(buffer.position() + 12);
        buffer.put(dirName);

        byte[] cd = buffer.array();

        List<String> result = ListRemoteZip.parseFileList(cd);

        assertEquals(1, result.size());
        assertEquals("file.zip", result.get(0));
    }

    @Test
    void testParseFileList_invalidSignature() {
        byte[] invalid = new byte[46];
        ByteBuffer.wrap(invalid).order(ByteOrder.LITTLE_ENDIAN).putInt(0x12345678);

        assertThrows(IOException.class, () -> ListRemoteZip.parseFileList(invalid));
    }

    @Test
    void testIsValidInput_valid() {
        String[] args = { "https://example.com/archive.zip" };
        assertDoesNotThrow(() -> ListRemoteZip.isValidInput(args));
    }

    @Test
    void testIsValidInput_missingArgs() {
        String[] args = {};
        assertThrows(IllegalArgumentException.class, () -> ListRemoteZip.isValidInput(args));
    }

    @Test
    void testIsValidInput_invalidExtension() {
        String[] args = { "https://example.com/file.txt" };
        assertThrows(IllegalArgumentException.class, () -> ListRemoteZip.isValidInput(args));
    }

    @Test
    void testIsValidInput_malformedUrl() {
        String[] args = { "https://ex ample.com/file.zip" };
        assertThrows(IllegalArgumentException.class, () -> ListRemoteZip.isValidInput(args));
    }
}
