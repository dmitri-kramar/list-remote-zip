import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ListRemoteZip {

    private static final int CENTRAL_DIRECTORY_HEADER_SIZE = 46;
    private static final int CD_ENTRY_SIGNATURE = 0x02014b50;
    private static final int UNSIGNED_SHORT_MASK = 0xFFFF;
    private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;

    private static final HttpClient CLIENT = HttpClient.newHttpClient();


    /**
     * Validates the input arguments to ensure a single .zip URL is provided.
     *
     * @param args command-line arguments
     * @throws IllegalArgumentException if the input is invalid
     */
    static void isValidInput(String[] args) throws IllegalArgumentException {
        String message = "incorrect input, use valid <URL>.zip";

        if (args.length != 1) {
            throw new IllegalArgumentException(message);
        }

        try {
            URI uri = new URI(args[0]);
            if (uri.getPath() == null || !uri.getPath().endsWith(".zip")) {
                throw new IllegalArgumentException(message);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Runs the ZIP listing process using the provided arguments.
     *
     * @param args command-line arguments
     * @throws IOException if a network or I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    private static void run(String[] args) throws IOException, InterruptedException {
        URI uri = URI.create(args[0]);
        long size = fetchFileSize(uri);
        byte[] eocd = fetchEndOfCentralDirectory(size, uri);
        byte[] cd = fetchCentralDirectory(eocd, uri);
        printFileList(parseFileList(cd));
    }

    /**
     * Fetches the size of the remote ZIP file via HTTP HEAD request.
     *
     * @param uri URI of the ZIP file
     * @return size of the file in bytes
     * @throws IOException if a network or header error occurs
     * @throws InterruptedException if the request is interrupted
     */
    private static long fetchFileSize(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = CLIENT
                .send(request, HttpResponse.BodyHandlers.discarding());

        String header = response.headers()
                .firstValue("Content-Length")
                .orElseThrow(() -> new IOException("ZIP file not found or is not accessible."));

        return Long.parseLong(header);
    }

    /**
     * Fetches the End of Central Directory (EOCD) section of the ZIP file.
     *
     * @param fileSize total size of the ZIP file
     * @param uri URI of the ZIP file
     * @return EOCD section as byte array
     * @throws IOException if a network error occurs
     * @throws InterruptedException if the request is interrupted
     */
    private static byte[] fetchEndOfCentralDirectory(long fileSize, URI uri) throws IOException, InterruptedException {
        long from = fileSize - 22;
        long to = fileSize - 1;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Range", "bytes=" + from + "-" + to)
                .build();

        HttpResponse<byte[]> response = CLIENT
                .send(request, HttpResponse.BodyHandlers.ofByteArray());

        return response.body();
    }

    /**
     * Fetches the Central Directory section of the ZIP file.
     *
     * @param eocd byte array of the EOCD section
     * @param uri URI of the ZIP file
     * @return Central Directory section as byte array
     * @throws IOException if a network error occurs
     * @throws InterruptedException if the request is interrupted
     */
    private static byte[] fetchCentralDirectory(byte[] eocd, URI uri) throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.wrap(eocd).order(ByteOrder.LITTLE_ENDIAN).position(12);

        long to = buffer.getInt() - 1 & UNSIGNED_INT_MASK;
        long from = buffer.getInt() & UNSIGNED_INT_MASK;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Range", "bytes=" + from + "-" + (from + to))
                .build();

        HttpResponse<byte[]> response = CLIENT
                .send(request, HttpResponse.BodyHandlers.ofByteArray());

        return response.body();
    }

    /**
     * Parses the Central Directory and extracts file names.
     *
     * @param cd byte array of the Central Directory
     * @return list of file names (excluding directories)
     * @throws IOException if the directory is invalid
     */
    static List<String> parseFileList(byte[] cd) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(cd).order(ByteOrder.LITTLE_ENDIAN);

        List<String> result = new ArrayList<>();

        while (buffer.remaining() >= CENTRAL_DIRECTORY_HEADER_SIZE) {
            if (buffer.getInt() != CD_ENTRY_SIGNATURE) {
                throw new IOException("invalid Central Directory signature");
            }

            buffer.position(buffer.position() + 24);
            int nameLength = buffer.getShort() & UNSIGNED_SHORT_MASK;
            int extraLength = buffer.getShort() & UNSIGNED_SHORT_MASK;
            int commentLength = buffer.getShort() & UNSIGNED_SHORT_MASK;
            buffer.position(buffer.position() + 12);
            byte[] fileName = new byte[nameLength];
            buffer.get(fileName);

            String fullFileName = new String(fileName, StandardCharsets.UTF_8);
            if (!fullFileName.endsWith("/")) {
                result.add(fullFileName);
            }

            buffer.position(buffer.position() + extraLength + commentLength);
        }

        return result;
    }

    /**
     * Prints the list of file names and total count.
     *
     * @param fileList list of file names to print
     */
    private static void printFileList(List<String> fileList) {
        System.out.println();
        fileList.forEach(System.out::println);
        System.out.printf("%nTotal files in archive: %d%n", fileList.size());
    }

    /**
     * Entry point of the program.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            isValidInput(args);
            run(args);
        } catch (InterruptedException e) {
            System.out.println("Error: operation was interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
