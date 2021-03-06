package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by Stefan Gugarel on 26/02/15.
 */
class ProvisioningInstallTaskOSXTest {

	Project project
	ProvisioningInstallTask provisioningInstallTask;

	GMockController mockControl
	CommandRunner commandRunnerMock

	File provisionLibraryPath
	File projectDir


	@BeforeMethod
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX

		provisioningInstallTask = project.getTasks().getByPath(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

		provisioningInstallTask.setProperty("commandRunner", commandRunnerMock)

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(projectDir)
	}


	void mockLinking(String name) {


		File source = new File(projectDir, "build/provision/" + name)
		File destination = new File(provisionLibraryPath, name)

		def commandList = ["/bin/ln", "-s", source.absolutePath , destination.absolutePath]
		commandRunnerMock.run(commandList)
	}

	@Test
	void singleProvisioningProfile() {

		File testMobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		project.xcodebuild.signing.mobileProvisionURI = testMobileprovision.toURI().toString()

		ProvisioningProfileIdReader provisioningProfileIdReader = new ProvisioningProfileIdReader(testMobileprovision.absolutePath, project)
		String uuid = provisioningProfileIdReader.getUUID()
		String name =  "gradle-" + uuid + ".provisionprofile";

		mockLinking(name)

		mockControl.play {
			provisioningInstallTask.install()
		}

		File sourceFile = new File(projectDir, "build/provision/" + name)
		assert sourceFile.exists()
	}

	@Test
	void multipleProvisioningProfiles() {

		File firstMobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		File secondMobileprovision = new File("src/test/Resource/openbakery-example.provisionprofile")
		project.xcodebuild.signing.mobileProvisionURI = [firstMobileprovision.toURI().toString(), secondMobileprovision.toURI().toString() ]

		String firstName = "gradle-" + new ProvisioningProfileIdReader(firstMobileprovision.absolutePath, project).getUUID() + ".provisionprofile";
		String secondName = "gradle-" + new ProvisioningProfileIdReader(secondMobileprovision.absolutePath, project).getUUID() + ".provisionprofile";

		mockLinking(firstName)
		mockLinking(secondName)

		mockControl.play {
			provisioningInstallTask.install()
		}

		File firstFile = new File(projectDir, "build/provision/" + firstName)
		assert firstFile.exists()

		File secondFile = new File(projectDir, "build/provision/" + secondName)
		assert secondFile.exists()
	}
}
