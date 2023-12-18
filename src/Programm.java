
import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import PathUtils.*;
import Telnet.TelnetController;

public class Programm {

	public static void main(String[] args) throws Exception {
		new Programm().UI();

	}

	void UI() {
		JFrame a = new JFrame("example");
		JTextField b = new JTextField("edureka");
		b.setBounds(50, 100, 200, 30);
		a.add(b);
		a.setSize(300, 300);
		a.setLayout(null);
		a.setVisible(true);
	}

	ArrayList<FileUnit> files = new ArrayList<FileUnit>();
	StringJoiner report = new StringJoiner("\n");
	int summary_size = 0;

	void logic() throws IOException {
		TelnetController tc = new TelnetController();
		tc.connect();
		tc.login();

		printFiles(FileUnit.getRoot(), tc, "/", 0);
		tc.disconnect();
		System.out.println(report);
		System.out.printf("Суммарный размер файлов: %s байт, %s кбайт, %s мбайт", summary_size, summary_size / 1024,
				summary_size / 1024 / 1024);

		Map<String, Integer> sizes = new HashMap<String, Integer>();

		for (FileUnit f : files) {
			if (sizes.containsKey(f.Extension)) {
				int size = sizes.get(f.Extension);
				size += f.Size;
				sizes.replace(f.Extension, size);
			} else {
				sizes.put(f.Extension, f.Size);
			}
		}

		for (var entry : sizes.entrySet()) {
			System.out.println(entry.getKey() + "/" + entry.getValue());
		}
	}

	void printFiles(FileUnit f, TelnetController tc, String path, int indent) throws IOException {

		report.add(tabs(indent) + f);
		if (f.IsFile) {
			files.add(f);
			summary_size += f.Size;
		}

		if (f.IsFolder) {
			String new_path = path + f.Name + "/";
			var file_list = tc.getFilesList(new_path);
			for (String file_line : file_list) {
				var nf = new FileUnit(file_line);
				printFiles(nf, tc, new_path, indent + 1);
			}
		}
	}

	static String tabs(int count) {
		return "	".repeat(count);
	}
}
