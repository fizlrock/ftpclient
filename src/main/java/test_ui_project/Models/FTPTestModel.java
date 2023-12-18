
package test_ui_project.Models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import test_ui_project.Models.Telnet.*;
import test_ui_project.Models.PathUtils.*;

public class FTPTestModel {

	public FTPTestModel(FTPController tc) {
		file_tree_report = new StringJoiner("\n");
		size_extension_table = new StringJoiner("\n");
		this.tc = tc;
	}

	ArrayList<FileUnit> files = new ArrayList<FileUnit>();
	public StringJoiner file_tree_report;
	public StringJoiner size_extension_table;
	int summary_size = 0;
	FTPController tc;

	public boolean exec() {
		boolean success = false;

		try {
			createFileTree(FileUnit.getRoot(), tc, "/", 0);
			System.out.println(file_tree_report);
			System.out.printf("Суммарный размер файлов: %s байт, %s кбайт, %s мбайт", summary_size, summary_size / 1024,
					summary_size / 1024 / 1024);
			success = true;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return success;
	}

	void create_size_extension_table() {

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

	void createFileTree(FileUnit f, FTPController tc, String path, int indent) throws IOException {
		file_tree_report.add(tabs(indent) + f);
		if (f.IsFile) {
			files.add(f);
			summary_size += f.Size;
		}

		if (f.IsFolder) {
			String new_path = path + f.Name + "/";
			var file_list = tc.getFilesList(new_path);
			for (String file_line : file_list) {
				var nf = new FileUnit(file_line);
				createFileTree(nf, tc, new_path, indent + 1);
			}
		}
	}

	static String tabs(int count) {
		return "	".repeat(count);
	}
}
