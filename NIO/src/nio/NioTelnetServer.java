package nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NioTelnetServer {

    private static final String HOST = "localhost";

    private static final int PORT = 8189;

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    private Path rootPath = Paths.get("").toRealPath();

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();

        server.bind(new InetSocketAddress(HOST, PORT));
        server.configureBlocking(false);

        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started!");

        while (server.isOpen()) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                if (key.isWritable()) {
                    System.out.println(key);
                }
                iterator.remove();
            }
        }
    }

    // TODO: 30.10.2020
    //  ls - список файлов (сделано на уроке),
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем
    //  mkdir (name) создать директорию
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        int read = channel.read(buffer);

        if (read == -1) {
            System.out.println(-1);
            channel.close();
            return;
        } else if (read == 0) {
            System.out.println(0);
            return;
        }

        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();

        String command = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");
        System.out.println(command);

        if (command.equals("--help")) {
            channel.write(ByteBuffer.wrap("input ls for show file list\n\r".getBytes()));
            channel.write(ByteBuffer.wrap("input cd <name> for change directory\n\r".getBytes()));
            channel.write(ByteBuffer.wrap("input touch <name> for create file\n\r".getBytes()));
            channel.write(ByteBuffer.wrap("input mkdir <name> for crete directory\n\r".getBytes()));
            channel.write(ByteBuffer.wrap("input rm <name> for delete file\n\r".getBytes()));
            channel.write(ByteBuffer.wrap("input copy <src> <dst> for delete file\n\r".getBytes()));
            channel.write(ByteBuffer.wrap("input cat <name> for delete file\n\r".getBytes()));
        } else if (command.equals("ls")) {
            channel.write(ByteBuffer.wrap((getFilesList() + "\n\r").getBytes()));
        } else if (command.startsWith("cd ")) {
            String strPath = checkCommandArgument(command, channel);

            if (strPath != null) {
                Path targetPath;

                if (strPath.equals(".")) targetPath = rootPath;
                else if (strPath.equals("..")) targetPath = rootPath.getParent();
                else targetPath = rootPath.resolve(Paths.get(strPath)).toRealPath();

                if (Files.isDirectory(targetPath)) {
                    rootPath = targetPath;
                    channel.write(ByteBuffer.wrap(("Current path: " + rootPath.toAbsolutePath() + "\n\r").getBytes()));
                } else {
                    channel.write(ByteBuffer.wrap("Incorrect path\n\r".getBytes()));
                }
            }
        } else if (command.startsWith("touch ")) {
            String strPath = checkCommandArgument(command, channel);

            if (strPath != null) {
                Path target = rootPath.resolve(Paths.get(strPath));

                if (Files.notExists(target)) Files.createFile(target);
                else channel.write(ByteBuffer.wrap("File already exist\n\r".getBytes()));
            }
        } else if (command.startsWith("mkdir ")) {
            String strPath = checkCommandArgument(command, channel);

            if (strPath != null) {
                Path target = rootPath.resolve(Paths.get(strPath));

                if (Files.notExists(target)) Files.createDirectory(target);
                else channel.write(ByteBuffer.wrap("Folder already exist\n\r".getBytes()));
            }
        } else if (command.startsWith("rm ")) {
            String strPath = checkCommandArgument(command, channel);

            if (strPath != null) {
                Path target = rootPath.resolve(Paths.get(strPath));

                if (Files.exists(target)) Files.delete(target);
                else channel.write(ByteBuffer.wrap("File does not exist\n\r".getBytes()));
            }
        } else if (command.startsWith("cat ")) {
            String strPath = checkCommandArgument(command, channel);

            if (strPath != null) {
                Path target = rootPath.resolve(Paths.get(strPath));

                if (Files.exists(target)) {
                   Files.readAllLines(target, StandardCharsets.UTF_8)
                           .forEach(line -> {
                               try {
                                   channel.write(ByteBuffer.wrap((line + "\n\r").getBytes()));
                               } catch (IOException e) {
                                   e.printStackTrace();
                               }
                           });
                }
                else channel.write(ByteBuffer.wrap("file does not exist\n\r".getBytes()));
            }
        } else if (command.startsWith("copy ")) {

        }
    }

    // TODO: 30.10.2020
    //  ls - список файлов (сделано на уроке),                          +
    //  cd (name) - перейти в папку                                     +
    //  touch (name) создать текстовый файл с именем                    +
    //  mkdir (name) создать директорию                                 +
    //  rm (name) удалить файл по имени                                 +
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла                 +

    private String checkCommandArgument(String command, SocketChannel channel) throws IOException {
        String argsPart = command.split(" ", 2)[1];

//        Pattern pattern = Pattern.compile("([\\w/] ?)|(\"[\\w/ ]\" ?)");
        Pattern pattern = Pattern.compile(" ?([\\w/_.!]+)|(\"[\\w/_.! ]+\") ?");
        Matcher matcher = pattern.matcher(argsPart);
        System.out.println(matcher.matches());
        System.out.println(matcher.start());

        return null;
    }

    private void sendMessage(String message, Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                ((SocketChannel)key.channel())
                        .write(ByteBuffer.wrap(message.getBytes()));
            }
        }
    }

    @SuppressWarnings("unckeked")
    private String getFilesList() {
        return String.join(" ", Objects.requireNonNull(new File(rootPath.toUri()).list()));
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();

        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "LOL");

        channel.write(ByteBuffer.wrap("Enter --help\n\r".getBytes()));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}
