package ru.fizteh.fivt.students.mishatkin.filemap;

import ru.fizteh.fivt.students.mishatkin.shell.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Vladimir Mishatkin on 10/14/13
 */
public class FileMapReceiver extends ShellReceiver implements FileMapReceiverProtocol {

	private File dbFile;
	private File dbFileOwningDirectory;
	private Map<String, String> dictionary = new HashMap<>();

	private Map<String, String> originalDictionaryPart = new HashMap<>();
	private Map<String, String> unstagedDictionaryPart = new HashMap<>();

	private int completelyNewKeysCount;

	private static final int TERRIBLE_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

	private boolean isValidStringLength(int size) {
		return size > 0 && size < TERRIBLE_FILE_SIZE;
	}

	public FileMapReceiver(String dbDirectory, String dbFileName, boolean interactiveMode, PrintStream out) throws FileMapDatabaseException {
		this(dbDirectory, dbFileName, interactiveMode, new ShellPrintStream(out));

	}

	public FileMapReceiver(String dbDirectory, String dbFileName, boolean interactiveMode, ShellPrintStream out) throws FileMapDatabaseException {
		super(out, interactiveMode);
		this.completelyNewKeysCount = 0;
		FileInputStream in = null;
		try {
			assert dbDirectory != null;
			dbFile = new File(new File( dbDirectory), dbFileName);
			if (!dbFile.exists() || (dbFile.exists() && dbFile.isDirectory())) {
				dbFile.createNewFile();
			}
			in = new FileInputStream(dbFile.getCanonicalFile());
			dbFileOwningDirectory = new File(dbDirectory);
		} catch (IOException e) {
			throw new FileMapDatabaseException("Some internal error.");
		}
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(in);
			boolean hasNext = true;
			while (hasNext) {
				try {
					dis.mark(1024 * 1024); // 1 MB
					int keyLength = dis.readInt();
					int valueLength = dis.readInt();
					if (!isValidStringLength(keyLength) || !isValidStringLength(valueLength)) {
						throw new FileMapDatabaseException("Invalid input key or value length in DB file.");
					}
					byte[] keyBinary = new byte[keyLength];
					byte[] valueBinary = new byte[valueLength];
					dis.read(keyBinary, 0, keyLength);
					dis.read(valueBinary, 0, valueLength);
					String key = new String(keyBinary, "UTF-8");
					String value = new String(valueBinary, "UTF-8");
					dictionary.put(key, value);
				} catch (EOFException e) {
					hasNext = false;
				} catch (IOException e) {
					throw new FileMapDatabaseException("DB file missing or corrupted.");
				}
			}
		} finally {
			try {
				if (dis != null) {
					dis.close();
				}
			} catch (NullPointerException | IOException ignored) {
			}
		}
	}
	public void showPrompt() {
		if (isInteractiveMode()) {
			print("$ ");
		}
	}

	@Override
	public String putCommand(String key, String value) {
		String oldValue = dictionary.get(key);
		if (oldValue != null) {
			if (value.equals(originalDictionaryPart.get(key))) {
				originalDictionaryPart.remove(key);
			} else {
				originalDictionaryPart.put(key, oldValue);
			}
			println("overwrite");
			println(oldValue);
		} else {
			unstagedDictionaryPart.put(key, value);
			++completelyNewKeysCount;
			println("new");
		}
		dictionary.put(key, value);
		return oldValue;
	}

	@Override
	public String removeCommand(String key) {
		String oldValue = dictionary.get(key);
		String retValue = dictionary.remove(key);
		if (retValue != null) {
			if (originalDictionaryPart.get(key) == null) {
				if (unstagedDictionaryPart.remove(key) != null) {
					--completelyNewKeysCount;
				}
				if (oldValue != null) {
					originalDictionaryPart.put(key, oldValue);
				}
			}
			println("removed");
		} else {
			println("not found");
		}
		return retValue;
	}

	@Override
	public String getCommand(String key) {
		String value = dictionary.get(key);
		if (value != null) {
			println("found");
			println(value);
		} else {
			println("not found");
		}
		return value;
	}

	public void exitCommand() throws TimeToExitException {
		try {
			writeChangesToFile();
		} catch (ShellException e) {
			System.out.println(e.getMessage());
		}
		super.exitCommand();
	}

	private void writeChangesToFile() throws ShellException {
		if (dictionary.isEmpty()) {
			try {
				changeDirectoryCommand(dbFileOwningDirectory.getAbsolutePath());
				rmCommand(dbFile.getAbsolutePath());
			} catch (ShellException probablyNoFileThere) {
			}
			return;
		}
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(dbFile));
			Set<String> keys = dictionary.keySet();
			for (String key : keys) {
				try {
					String value = dictionary.get(key);
					dos.writeInt(key.getBytes().length);
					dos.writeInt(value.getBytes().length);
					dos.write(key.getBytes());
					dos.write(value.getBytes());
				} catch (IOException e) {
					System.err.println("Internal error.");
				}
			}
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
			throw new ShellException("OK, now someone just took the file out of me, so I cannot even rewrite it.");
		} finally {
			try {
				if (dos != null) {
					dos.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	public boolean doHashCodesConformHash(int hashCodeRemainder, int secondRadixHashCodeRemainder, int mod) {
		boolean doConform = true;
		for (String key : dictionary.keySet()) {
			int code = key.hashCode();
			if (Math.abs(code % mod) != hashCodeRemainder ||
				Math.abs((code / mod) % mod)!= secondRadixHashCodeRemainder) {
				return false;
			}
		}
		return doConform;
	}

	public int getUnstagedChangesCount() {
		int match = 0;
		for (String key : originalDictionaryPart.keySet()) {
			if (dictionary.get(key) != null && dictionary.get(key).equals(originalDictionaryPart.get(key))) {
				++match;
			}
		}
		return originalDictionaryPart.size() + completelyNewKeysCount - match;
	}

	public int size() {
		//TODO: modify this as new owning maps appear according to their states
		//or mby not
		return dictionary.size();
	}

	public int commit() throws ShellException {
		int retValue = getUnstagedChangesCount();
		writeChangesToFile();
		originalDictionaryPart.clear();
		unstagedDictionaryPart.clear();
		return retValue;
	}

	public int rollback() {
		int retValue = getUnstagedChangesCount();
		for (String key : unstagedDictionaryPart.keySet()) {
			dictionary.remove(key);
		}
		for (String key : originalDictionaryPart.keySet()) {
			dictionary.put(key, originalDictionaryPart.get(key));
		}
		originalDictionaryPart.clear();
		unstagedDictionaryPart.clear();
		return retValue;
	}
}
