package jadx.gui.ui;

import jadx.core.LOGS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

/**
 * Enables drop support from external applications for the {@link MainWindow} (load dropped APK file)
 */
public class MainDropTarget implements DropTargetListener {

	private static final Logger LOG = LoggerFactory.getLogger(MainDropTarget.class);

	private final MainWindow mainWindow;

	public MainDropTarget(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	protected void processDrag(DropTargetDragEvent dtde) {
		if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
		} else {
			dtde.rejectDrag();
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		processDrag(dtde);
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		processDrag(dtde);
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent dtde) {
		if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.rejectDrop();
			return;
		}
		dtde.acceptDrop(dtde.getDropAction());
		try {
			Transferable transferable = dtde.getTransferable();
			List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
			if (transferData != null && transferData.size() > 0) {
				dtde.dropComplete(true);
				// load first file
				mainWindow.openFile(transferData.get(0));
			}
		} catch (Exception e) {
			LOGS.error("File drop operation failed", e);
		}
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	}
}
