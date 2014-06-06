package com.github.katjahahn.sections;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.github.katjahahn.FileFormatException;
import com.github.katjahahn.PEData;
import com.github.katjahahn.PELoader;
import com.github.katjahahn.PELoaderTest;
import com.github.katjahahn.TestreportsReader.TestData;
import com.github.katjahahn.optheader.DataDirEntry;
import com.github.katjahahn.optheader.DataDirectoryKey;
import com.github.katjahahn.sections.edata.ExportSection;
import com.github.katjahahn.sections.idata.ImportSection;
import com.github.katjahahn.sections.rsrc.ResourceSection;

public class SectionLoaderTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager
			.getLogger(SectionLoaderTest.class.getName());
	private List<TestData> testdata;
	private Map<String, PEData> pedata = new HashMap<>();

	@BeforeClass
	public void prepare() throws IOException {
		testdata = PELoaderTest.getTestData();
		pedata = PELoaderTest.getPEData();
	}

	@Test
	public void constructorTest() throws FileFormatException {
		PEData datum = pedata.get("strings.exe");
		SectionLoader loader1 = new SectionLoader(datum);
		SectionLoader loader2 = new SectionLoader(datum.getSectionTable(),
				datum.getOptionalHeader(), datum.getFile());
		for (DataDirectoryKey key : DataDirectoryKey.values()) {
			Long offset1 = loader1.getFileOffsetFor(key);
			Long offset2 = loader2.getFileOffsetFor(key);
			assertEquals(offset1, offset2);
		}
	}
	
	@Test
	public void unableToLoadImports() throws IOException {
		File file = new File("src/main/resources/x64viruses/VirusShare_baed21297974b6adf3298585baa78691");
		PEData data = PELoader.loadPE(file);
		SectionLoader loader = new SectionLoader(data);
		assertNull(loader.loadImportSection());
	}

	@Test
	public void unableToLoadResources() throws IOException {
		File file = new File("src/main/resources/x64viruses/VirusShare_baed21297974b6adf3298585baa78691");
		PEData data = PELoader.loadPE(file);
		SectionLoader loader = new SectionLoader(data);
		assertNull(loader.loadResourceSection());
	}


	@Test
	public void getSectionEntryByRVA() {
		for (PEData datum : pedata.values()) {
			SectionTable table = datum.getSectionTable();
			SectionLoader loader = new SectionLoader(datum);
			for (SectionHeader entry : table.getSectionHeaders()) {
				long start = entry.get(SectionHeaderKey.VIRTUAL_ADDRESS);
				long size = entry.get(SectionHeaderKey.VIRTUAL_SIZE);
				SectionHeader actual = loader.getSectionHeaderByRVA(start);
				assertEquals(actual, entry);
				actual = loader.getSectionHeaderByRVA(start + size - 1);
				assertEquals(actual, entry);
				actual = loader.getSectionHeaderByRVA(size / 2 + start);
				assertEquals(actual, entry);
			}
		}
	}

	@Test
	public void loadExportSection() throws IOException {
		for (TestData testdatum : testdata) {
			List<DataDirEntry> testDirs = testdatum.dataDir;
			PEData pedatum = pedata.get(testdatum.filename.replace(".txt", ""));
			for (DataDirEntry testDir : testDirs) {
				if (testDir.key.equals(DataDirectoryKey.EXPORT_TABLE)) {
					ExportSection edata = new SectionLoader(pedatum)
							.loadExportSection();
					assertNotNull(edata);
				}
			}
		}
	}

	@Test
	public void loadImportSection() throws IOException {
		for (TestData testdatum : testdata) {
			List<DataDirEntry> testDirs = testdatum.dataDir;
			PEData pedatum = pedata.get(testdatum.filename.replace(".txt", ""));
			for (DataDirEntry testDir : testDirs) {
				if (testDir.key.equals(DataDirectoryKey.IMPORT_TABLE)) {
					ImportSection idata = new SectionLoader(pedatum)
							.loadImportSection();
					assertNotNull(idata);
				}
			}
		}
	}

	@Test
	public void loadResourceSection() throws IOException {
		for (TestData testdatum : testdata) {
			List<DataDirEntry> testDirs = testdatum.dataDir;
			PEData pedatum = pedata.get(testdatum.filename.replace(".txt", ""));
			for (DataDirEntry testDir : testDirs) {
				if (testDir.key.equals(DataDirectoryKey.RESOURCE_TABLE)) {
					ResourceSection rsrc = new SectionLoader(pedatum)
							.loadResourceSection();
					assertNotNull(rsrc);
				}
			}
		}
	}

	@Test
	public void loadSectionWithSizeAnomaly() throws IOException {
		PEData datum = pedata.get("Lab05-01.dll");
		new SectionLoader(datum).loadSection(".reloc");
	}

	@Test
	public void loadSectionByName() throws IOException {
		for (PEData datum : pedata.values()) {
			SectionLoader loader = new SectionLoader(datum);
			SectionTable table = datum.getSectionTable();
			for (SectionHeader header : table.getSectionHeaders()) {
				String name = header.getName();
				PESection section = loader.loadSection(name);
				assertNotNull(section);
				assertEquals(section.getDump().length,
						(int) loader.getReadSize(header));
			}
		}
	}

	@Test
	public void loadSectionByNumber() throws IOException {
		for (PEData datum : pedata.values()) {
			SectionLoader loader = new SectionLoader(datum);
			SectionTable table = datum.getSectionTable();
			for (SectionHeader header : table.getSectionHeaders()) {
				PESection section = loader.loadSection(header.getNumber());
				assertNotNull(section);
				assertEquals(section.getDump().length,
						(int) loader.getReadSize(header));
			}
		}
	}
}