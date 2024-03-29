package hu.keratomi.moneysavingcalculator.util;

import androidx.core.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.String.join;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
    private static final String APPLICATION_FOLDER_NAME = "moneysavingcalculator";

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    private String applicationFolderId;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public Task<Void> createFolderForApplication(String folderId) {
        return Tasks.call(mExecutor, () -> {
            if (Strings.isNullOrEmpty(folderId)) {
                File fileMetadata = new File();
                fileMetadata.setName(APPLICATION_FOLDER_NAME);
                fileMetadata.setMimeType("application/vnd.google-apps.folder");

                File googleFile = mDriveService.files().create(fileMetadata)
                        .setFields("id")
                        .execute();
                if (googleFile == null) {
                    throw new IOException("Null result when requesting file creation.");
                }
                applicationFolderId = googleFile.getId();
            } else {
                applicationFolderId = folderId;
            }

            return null;
        });
    }

    public Task<String> uploadFile(String name, java.io.File fileTest) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList(applicationFolderId))
                    .setMimeType("text/plain")
                    .setName(name);

            InputStream targetStream = new FileInputStream(fileTest);
            InputStreamContent inputStreamContent = new InputStreamContent("text/plain", targetStream);
            File googleFile = mDriveService.files().create(metadata, inputStreamContent).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            return googleFile.getId();
        });
    }

    public Task<String> updateFile(String id, java.io.File fileTest) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File();

            InputStream targetStream = new FileInputStream(fileTest);
            InputStreamContent inputStreamContent = new InputStreamContent("text/plain", targetStream);
            File googleFile = mDriveService.files().update(id, metadata, inputStreamContent).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            return googleFile.getId();
        });
    }

    public Task<Void> deleteFile(String id) {
        return Tasks.call(mExecutor, () -> mDriveService.files().delete(id).execute());
    }


    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = mDriveService.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                List<String> lines = new ArrayList<>();
                String line;

                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }

                return Pair.create(name, join(System.lineSeparator(), lines));
            }
        });
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, () ->
                mDriveService.files().list().setSpaces("drive").execute());
    }

}
