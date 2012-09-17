/***************************************************************************
 *
 * This file is part of the NFC Eclipse Plugin project at
 * http://code.google.com/p/nfc-eclipse-plugin/
 *
 * Copyright (C) 2012 by Thomas Rorvik Skjolberg / Antares Gruppen AS.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ****************************************************************************/

package com.antares.nfc.model.editing;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.nfctools.ndef.ext.UnsupportedExternalTypeRecord;
import org.nfctools.ndef.unknown.UnknownRecord;

import com.antares.nfc.model.NdefRecordModelNode;
import com.antares.nfc.model.NdefRecordModelProperty;
import com.antares.nfc.plugin.operation.DefaultNdefModelPropertyOperation;
import com.antares.nfc.plugin.operation.NdefModelOperation;

public class ExternalTypeRecordEditingSupport extends DefaultRecordEditingSupport {

	public ExternalTypeRecordEditingSupport(
			TreeViewer treeViewer) {
		super(treeViewer);
	}

	@Override
	public NdefModelOperation setValue(NdefRecordModelNode node, Object value) {
		UnsupportedExternalTypeRecord unsupportedExternalTypeRecord = (UnsupportedExternalTypeRecord) node.getRecord();
		if(node instanceof NdefRecordModelProperty) {
			String stringValue = (String)value;
			
			int parentIndex = node.getParentIndex();
			if(parentIndex == 0) {
				if(!stringValue.equals(unsupportedExternalTypeRecord.getDomain())) {
					return new DefaultNdefModelPropertyOperation<String, UnsupportedExternalTypeRecord>(unsupportedExternalTypeRecord, (NdefRecordModelProperty)node, unsupportedExternalTypeRecord.getDomain(), stringValue) {
						
						@Override
						public void execute() {
							super.execute();
							
							record.setDomain(next);
						}
						
						@Override
						public void revoke() {
							super.revoke();
							
							record.setDomain(previous);
						}
					};
					
				}
			} else if(parentIndex == 1) {
				if(!stringValue.equals(unsupportedExternalTypeRecord.getType())) {
					return new DefaultNdefModelPropertyOperation<String, UnsupportedExternalTypeRecord>(unsupportedExternalTypeRecord, (NdefRecordModelProperty)node, unsupportedExternalTypeRecord.getType(), stringValue) {
						
						@Override
						public void execute() {
							super.execute();
							
							record.setType(next);
						}
						
						@Override
						public void revoke() {
							super.revoke();
							
							record.setType(previous);
						}
					};
					
				}
			} else if(parentIndex == 2) {				
				
				String path = (String)value;
				
				File file = new File(path);

				int length = (int)file.length();
				
				byte[] payload = new byte[length];
				
				DataInputStream din = null;
				try {
					din = new DataInputStream(new FileInputStream(file));
					
					din.readFully(payload);
				} catch(IOException e) {
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					MessageDialog.openError(shell, "Error", "Could not read file '" + file + "', reverting to previous value.");
					
					return null;
				} finally {
					if(din != null) {
						try {
							din.close();
						} catch(IOException e) {
							// ignore
						}
					}
				}
				
				return new DefaultNdefModelPropertyOperation<byte[], UnsupportedExternalTypeRecord>(unsupportedExternalTypeRecord, (NdefRecordModelProperty)node, unsupportedExternalTypeRecord.getData(), payload) {
					
					@Override
					public void execute() {
						super.execute();
						
						record.setData(next);
						
						if(next == null) {
							ndefRecordModelProperty.setValue("Zero bytes");
						} else {
							ndefRecordModelProperty.setValue(Integer.toString(next.length) + " bytes");
						}	

					}
					
					@Override
					public void revoke() {
						super.revoke();
						
						record.setData(previous);
						
						if(previous == null) {
							ndefRecordModelProperty.setValue("Zero bytes");
						} else {
							ndefRecordModelProperty.setValue(Integer.toString(previous.length) + " bytes");
						}	
					}
				};				
				
			}
			
			return null;
		} else {
			return super.setValue(node, value);
		}
	}

	@Override
	public Object getValue(NdefRecordModelNode node) {
		UnsupportedExternalTypeRecord unsupportedExternalTypeRecord = (UnsupportedExternalTypeRecord) node.getRecord();
		if(node instanceof NdefRecordModelProperty) {
			
			int parentIndex = node.getParentIndex();
			if(parentIndex == 0) {
				if(unsupportedExternalTypeRecord.hasDomain()) {
					return unsupportedExternalTypeRecord.getDomain();
				} else {
					return EMPTY_STRING;
				}
			} else if(parentIndex == 1) {
				if(unsupportedExternalTypeRecord.hasType()) {
					return unsupportedExternalTypeRecord.getType();
				} else {
					return EMPTY_STRING;
				}
			} else if(parentIndex == 2) {
				return EMPTY_STRING;
			} else {
				throw new RuntimeException();
			}
		} else {
			return super.getValue(node);
		}
	}

	@Override
	public CellEditor getCellEditor(NdefRecordModelNode node) {
		if(node instanceof NdefRecordModelProperty) {
			int parentIndex = node.getParentIndex();
			if(parentIndex == 2) {
				return new FileDialogCellEditor(treeViewer.getTree());
			} else {
				return new TextCellEditor(treeViewer.getTree());
			}
		} else {
			return super.getCellEditor(node);
		}
	}
}