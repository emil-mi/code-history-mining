package historyreader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import util.PastToPresentIterator
import util.PresentToPastIterator

import static util.Measure.measure

class CommitReader {
	private static final Logger LOG = Logger.getInstance(CommitReader.class.name)
	static Commit NO_MORE_COMMITS = null

	private final Project project
	private final int sizeOfVCSRequestInDays

	CommitReader(Project project, int sizeOfVCSRequestInDays = 30) {
		this.project = project
		this.sizeOfVCSRequestInDays = sizeOfVCSRequestInDays
	}

	Iterator<Commit> readCommits(Date historyStart, Date historyEnd, boolean readPresentToPast = true) {
		assert historyStart.time < historyEnd.time

		Iterator dateIterator = (readPresentToPast ?
			new PresentToPastIterator(historyStart, historyEnd, sizeOfVCSRequestInDays) :
			new PastToPresentIterator(historyStart, historyEnd, sizeOfVCSRequestInDays))
		List<Commit> changes = []

		new Iterator<Commit>() {
			@Override boolean hasNext() {
				!changes.empty || dateIterator.hasNext()
			}

			@Override Commit next() {
				if (!changes.empty) return changes.remove(0)

				measure("VCS request time") {
					while (changes.empty && dateIterator.hasNext()) {
						def dates = dateIterator.next()
						try {
							changes = requestCommitsFor(project, dates.from, dates.to)
						} catch (Exception e) { 
							// this is to catch errors in VCS plugin implementation 
							// e.g. this one http://youtrack.jetbrains.com/issue/IDEA-105360
              LOG.warn("Error while reading commits from ${dates.from} to ${dates.to}", e)
            }
						if (!readPresentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? NO_MORE_COMMITS : changes.remove(0)
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	static List<Commit> requestCommitsFor(Project project, Date fromDate = null, Date toDate = null) {
		def vcsRoots = vcsRootsIn(project)
		if (vcsRoots.empty) return []
		def vcsRoot = vcsRoots.first()

		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))
		if (changesProvider.class.simpleName == "GitCommittedChangeListProvider") {
			// TODO "location" can be null for git project when current branch is local (might be IntelliJ bug); https://github.com/dkandalov/code-history-mining/issues/5
			return GitPluginWorkaround.getCommittedChanges_with_intellij_git_api_workarounds(project, location, fromDate, toDate)
		}

		def settings = changesProvider.createDefaultSettings()
		if (fromDate != null) {
			settings.USE_DATE_AFTER_FILTER = true
			settings.dateAfter = fromDate
		}
		if (toDate != null) {
			settings.USE_DATE_BEFORE_FILTER = true
			settings.dateBefore = toDate
		}
		changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)
	}

	static amountOfVCSRootsIn(Project project) {
		vcsRootsIn(project).size()
	}

	private static List<VcsRoot> vcsRootsIn(Project project) {
		ProjectRootManager.getInstance(project).contentSourceRoots
				.collect{ ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(it) }
				.findAll{ it.path != null }.unique()
	}
}