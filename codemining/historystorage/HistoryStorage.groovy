package codemining.historystorage

import codemining.core.common.langutil.Cancelled
import codemining.core.common.langutil.JBFileUtil
import codemining.core.common.langutil.Measure
import codemining.core.historystorage.EventStorage
import org.jetbrains.annotations.Nullable

class HistoryStorage {
	private final String basePath
	private final Log log
	private final Measure measure

	HistoryStorage(String basePath = null, Measure measure = null, @Nullable Log log = null) {
		this.basePath = basePath
		this.measure = measure
		this.log = log
	}

	File[] filesWithCodeHistory() {
		new File(basePath).listFiles(new FileFilter() {
			@Override boolean accept(File pathName) { pathName.name.endsWith(".csv") }
		})
	}

	HistoryGrabberConfig loadGrabberConfigFor(String projectName) {
		HistoryGrabberConfig.loadGrabberConfigFor(projectName, basePath) {
			HistoryGrabberConfig.defaultConfig().withOutputFilePath("${basePath}/${projectName + "-file-events.csv"}")
		}
	}

	def saveGrabberConfigFor(String projectName, HistoryGrabberConfig config) {
		HistoryGrabberConfig.saveGrabberConfigOf(projectName, basePath, config)
	}

	boolean isValidName(String fileName) {
		fileName.length() > 0 && !new File("$basePath/$fileName").exists()
	}

	def rename(String fileName, String newFileName) {
		JBFileUtil.rename(new File("$basePath/$fileName"), new File("$basePath/$newFileName"))
	}

	def delete(String fileName) {
		JBFileUtil.delete(new File("$basePath/$fileName"))
	}

	def readAllEvents(String fileName, Cancelled checkIfCancelled) {
		measure.measure("Storage.readAllEvents"){
			new EventStorage("$basePath/$fileName").init().readAllEvents(checkIfCancelled){ line, e -> log?.failedToRead(line) }
		}
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	String guessProjectNameFrom(String fileName) {
		fileName.replace(".csv", "").replace("-file-events", "")
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	EventStorage eventStorageFor(String filePath) {
		new EventStorage(filePath).init()
	}

	interface Log {
		def failedToRead(def line)
	}
}
