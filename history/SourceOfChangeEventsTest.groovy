package history

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.changes.Change
import org.junit.Test

import static history.SourceOfChangeLists.requestChangeListsFor
import static history.util.DateTimeUtil.dateTime

class SourceOfChangeEventsTest {
	private final Project jUnitProject

	SourceOfChangeEventsTest() {
		jUnitProject = ProjectManager.instance.openProjects.find{it.name == "junit"}
		assert jUnitProject != null : "Couldn't find open project with junit project (which is required for this test)"
	}

	@Test "should treat renamed file as a single event"() {
		def changeLists = requestChangeListsFor(jUnitProject, dateTime("15:40 03/10/2007"), dateTime("15:45 03/10/2007"))
		def change = changeLists.first().changes.find{ it.beforeRevision.file.name.contains("TheoryMethod") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "TheoryMethod.java"
		assert change.afterRevision.file.name == "TheoryMethodRunner.java"
	}

	@Test "should treat moved file as a single event"() {
		def changeLists = requestChangeListsFor(jUnitProject, dateTime("08:50 28/07/2011"), dateTime("09:00 28/07/2011"))
		def change = changeLists.first().changes.find{ it.beforeRevision.file.name.contains("RuleFieldValidator") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "RuleFieldValidator.java"
		assert change.afterRevision.file.name == "RuleFieldValidator.java"

	}
}
