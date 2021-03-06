package org.icij.datashare;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.icij.datashare.Extension.Type.WEB;
import static org.icij.datashare.PropertiesProvider.EXTENSIONS_DIR;

public class ExtensionServiceTest {
    @Rule public TemporaryFolder extensionFolder = new TemporaryFolder();
    @Rule public TemporaryFolder otherFolder = new TemporaryFolder();
    @Test
    public void test_get_list() {
        Set<Extension> extensions = new ExtensionService(extensionFolder.getRoot().toPath()).list();
        assertThat(extensions).hasSize(3);
        assertThat(extensions.stream().map(Extension::getId).collect(Collectors.toSet()))
                .containsOnly("my-extension-foo", "my-extension-baz", "my-extension-bar");
    }

    @Test
    public void test_get_extension() {
        Set<Extension> extensions = new ExtensionService(extensionFolder.getRoot().toPath()).list("my-extension-baz");
        assertThat(extensions).hasSize(1);
        assertThat(extensions.iterator().next().type).isEqualTo(WEB);
    }

    @Test
    public void test_extension_service_from_properties() {
        ExtensionService extensionService = new ExtensionService(new PropertiesProvider(new HashMap<String, String>() {{
            put(EXTENSIONS_DIR, extensionFolder.getRoot().getPath());
        }}));
        assertThat(extensionService.extensionsDir.toString()).isEqualTo(extensionFolder.getRoot().getPath());
    }

    @Test
    public void test_install_an_extension_from_id() throws IOException {
        otherFolder.newFile("my-extension.jar");
        ExtensionService extensionService = new ExtensionService(extensionFolder.getRoot().toPath(), new ByteArrayInputStream(("{\"deliverableList\": [" +
                       "{\"id\":\"my-extension\", \"url\": \"" + otherFolder.getRoot().toPath().resolve("my-extension.jar").toUri() + "\"}" +
                       "]}").getBytes()));

        extensionService.downloadAndInstall("my-extension");

        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension.jar").toFile()).exists();
    }

    @Test
    public void test_download_and_install_extension_removes_temporary_file() throws Exception {
        otherFolder.newFile("extension.jar");

        Extension extension = new Extension(otherFolder.getRoot().toPath().resolve("extension.jar").toUri().toURL());
        File tmpFile = extension.download();
        extension.install(tmpFile, extensionFolder.getRoot().toPath());

        assertThat(tmpFile.getName()).startsWith("tmp");
        assertThat(tmpFile).doesNotExist();
        assertThat(extensionFolder.getRoot().toPath().resolve("extension.jar").toFile()).exists();
    }

    @Test
    public void test_install_an_extension_from_url() throws IOException {
        otherFolder.newFile("my-extension.jar");
        ExtensionService extensionService = new ExtensionService(extensionFolder.getRoot().toPath());

        extensionService.downloadAndInstall(otherFolder.getRoot().toPath().resolve("my-extension.jar").toUri().toURL());

        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension.jar").toFile()).exists();
    }

    @Test
    public void test_install_an_extension_from_file() throws IOException {
        otherFolder.newFile("my-extension.jar");

        new Extension(otherFolder.getRoot().toPath().resolve("my-extension.jar").toUri().toURL()).install(extensionFolder.getRoot().toPath());

        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension.jar").toFile()).exists();
    }

    @Test
    public void test_delete_previous_extension_if_version_differs() throws Exception {
        otherFolder.newFile("my-extension-1.2.3.jar");
        otherFolder.newFile("my-extension-1.2.4.jar");
        ExtensionService extensionService = new ExtensionService(extensionFolder.getRoot().toPath());

        extensionService.downloadAndInstall(otherFolder.getRoot().toPath().resolve("my-extension-1.2.3.jar").toUri().toURL());
        extensionService.downloadAndInstall(otherFolder.getRoot().toPath().resolve("my-extension-1.2.4.jar").toUri().toURL());

        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension-1.2.4.jar").toFile()).exists();
        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension-1.2.3.jar").toFile()).doesNotExist();
    }

    @Test
    public void test_delete_extension_by_id() throws IOException {
        extensionFolder.newFile( "my-extension.jar");
        ExtensionService extensionService = new ExtensionService(extensionFolder.getRoot().toPath(), new ByteArrayInputStream(("{\"deliverableList\": [" +
                "{\"id\":\"my-extension\", \"url\": \"" + otherFolder.getRoot().toPath().resolve("my-extension.jar").toUri() + "\"}" +
                "]}").getBytes()));
        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension.jar").toFile()).exists();

        extensionService.delete("my-extension");
        assertThat(extensionFolder.getRoot().toPath().resolve("my-extension.jar").toFile()).doesNotExist();

    }
}
