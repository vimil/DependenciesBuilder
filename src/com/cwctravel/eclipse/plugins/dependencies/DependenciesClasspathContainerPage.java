package com.cwctravel.eclipse.plugins.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.cwctravel.eclipse.plugins.dependencies.resolvers.IDependenciesResolver;

public class DependenciesClasspathContainerPage extends WizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {
	private String dependenciesId;
	private IJavaProject javaProject;
	private List<DependencyInfo> dependencies;
	private IDependenciesResolver resolver;

	private TableViewer dependenciesViewer;

	private static class DependencyPathEditingSupport extends EditingSupport {
		private CellEditor cellEditor;
		private IJavaProject currentJavaProject;

		private DependencyPathEditingSupport(ColumnViewer viewer, final IJavaProject currentJavaProject) {
			super(viewer);
			this.currentJavaProject = currentJavaProject;

			cellEditor = new CellEditor((Composite)viewer.getControl()) {
				private Text editor;

				@Override
				protected Control createControl(Composite parent) {
					editor = new Text(parent, SWT.SINGLE);
					return editor;
				}

				@Override
				protected Object doGetValue() {
					return editor.getText();
				}

				@Override
				protected void doSetFocus() {
					editor.setFocus();
				}

				@Override
				protected void doSetValue(Object value) {
					editor.setText((String)value);
				}

				@Override
				public LayoutData getLayoutData() {
					LayoutData data = new LayoutData();
					data.minimumWidth = 0;
					return data;
				}
			};
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			DependencyInfo dependencyInfo = (DependencyInfo)element;
			if(dependencyInfo != null) {
				return dependencyInfo.getRawPath();
			}
			return "";
		}

		@Override
		protected void setValue(Object element, Object value) {
			DependencyInfo dependencyInfo = (DependencyInfo)element;
			if(dependencyInfo != null) {
				String dependencyPath = (String)value;
				String resolvedPath = DependenciesUtil.resolvePath(currentJavaProject.getProject(), dependencyPath);
				if(resolvedPath != null) {
					dependencyInfo.setPath(dependencyPath, resolvedPath);
					getViewer().refresh(element);
				}
			}
		}
	}

	private static class DependencyAssociatedProjectEditingSupport extends EditingSupport {
		private CellEditor cellEditor;

		private DependencyAssociatedProjectEditingSupport(final TableViewer viewer, final IJavaProject currentJavaProject) {
			super(viewer);

			cellEditor = new CellEditor((Composite)viewer.getControl()) {
				private Combo combo;

				@Override
				protected Control createControl(Composite parent) {
					combo = new Combo(parent, SWT.DROP_DOWN);

					combo.add(DependenciesConstants.ASSOCIATED_PROJECT_NONE);
					try {
						List<IJavaProject> javaProjects = DependenciesUtil.getJavaProjectsInWorkspace();
						for(IJavaProject javaProject: javaProjects) {
							if(currentJavaProject == null || !currentJavaProject.getPath().equals(javaProject.getPath())) {
								combo.add(javaProject.getPath().toPortableString());
							}
						}

					}
					catch(CoreException e) {
						DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
					}
					return combo;
				}

				@Override
				protected Object doGetValue() {
					String value = combo.getText();
					if(value != null && !value.isEmpty()) {
						if(findItem(value, combo.getItems()) < 0) {
							combo.add(value);
						}
						return value;
					}
					return DependenciesConstants.ASSOCIATED_PROJECT_NONE;
				}

				@Override
				protected void doSetFocus() {
					combo.setFocus();
				}

				@Override
				protected void doSetValue(Object value) {

					TableItem[] tableItems = viewer.getTable().getItems();
					for(TableItem tableItem: tableItems) {
						DependencyInfo dependencyInfo = (DependencyInfo)tableItem.getData();
						String associatedProject = dependencyInfo.getAssociatedProject();
						if(findItem(associatedProject, combo.getItems()) < 0) {
							combo.add(associatedProject);
						}
					}

					boolean itemFound = findItem(value, combo.getItems()) >= 0;

					if(!itemFound) {
						combo.add((String)value);
						combo.select(combo.getItemCount() - 1);
					}
				}

				private int findItem(Object value, String[] items) {
					for(int i = 0; i < items.length; i++) {
						if(items[i].equals(value)) {
							combo.select(i);
							return i;
						}
					}
					return -1;
				}

				@Override
				public LayoutData getLayoutData() {
					LayoutData data = new LayoutData();
					data.minimumWidth = 0;
					return data;
				}
			};
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			DependencyInfo dependencyInfo = (DependencyInfo)element;
			if(dependencyInfo != null) {
				return dependencyInfo.getAssociatedProject();
			}
			return DependenciesConstants.ASSOCIATED_PROJECT_NONE;
		}

		@Override
		protected void setValue(Object element, Object value) {
			DependencyInfo dependencyInfo = (DependencyInfo)element;
			if(dependencyInfo != null) {
				dependencyInfo.setAssociatedProject((String)value);
				getViewer().refresh(element);
			}
		}
	}

	private static class DependencyScopeEditingSupport extends EditingSupport {
		private CellEditor cellEditor;

		private DependencyScopeEditingSupport(ColumnViewer viewer) {
			super(viewer);
			cellEditor = new CellEditor((Composite)viewer.getControl()) {
				private Combo combo;

				@Override
				protected Control createControl(Composite parent) {
					combo = new Combo(parent, SWT.READ_ONLY);
					combo.add(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_PROVIDED);
					combo.add(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_COMPILE);
					combo.add(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_TEST);
					combo.add(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_RUNTIME);
					combo.add(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_SYSTEM);
					return combo;
				}

				@Override
				protected Object doGetValue() {
					int selectionIndex = combo.getSelectionIndex();
					switch(selectionIndex) {
						case 0:
							return DependenciesConstants.DEPENDENCY_SCOPE_PROVIDED;
						case 1:
							return DependenciesConstants.DEPENDENCY_SCOPE_COMPILE;
						case 2:
							return DependenciesConstants.DEPENDENCY_SCOPE_TEST;
						case 3:
							return DependenciesConstants.DEPENDENCY_SCOPE_RUNTIME;
						case 4:
							return DependenciesConstants.DEPENDENCY_SCOPE_SYSTEM;

						default:
							return DependenciesConstants.DEPENDENCY_SCOPE_NONE;
					}
				}

				@Override
				protected void doSetFocus() {
					combo.setFocus();
				}

				@Override
				protected void doSetValue(Object value) {
					int dependencyScope = (Integer)value;
					switch(dependencyScope) {
						case DependenciesConstants.DEPENDENCY_SCOPE_PROVIDED: {
							combo.select(0);
							break;
						}
						case DependenciesConstants.DEPENDENCY_SCOPE_COMPILE: {
							combo.select(1);
							break;
						}
						case DependenciesConstants.DEPENDENCY_SCOPE_TEST: {
							combo.select(2);
							break;
						}
						case DependenciesConstants.DEPENDENCY_SCOPE_RUNTIME: {
							combo.select(3);
							break;
						}
						case DependenciesConstants.DEPENDENCY_SCOPE_SYSTEM: {
							combo.select(4);
							break;
						}
					}
				}

				@Override
				public LayoutData getLayoutData() {
					LayoutData data = new LayoutData();
					data.minimumWidth = 0;
					return data;
				}
			};

		}

		@Override
		protected void setValue(Object element, Object value) {
			DependencyInfo dependencyInfo = (DependencyInfo)element;
			if(dependencyInfo != null) {
				dependencyInfo.setScope((Integer)value);
				getViewer().refresh(element);
			}

		}

		@Override
		protected Object getValue(Object element) {
			DependencyInfo dependencyInfo = (DependencyInfo)element;
			if(dependencyInfo != null) {
				return dependencyInfo.getScope();
			}
			return -1;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}

	private static class DependenciesContentProvider implements IStructuredContentProvider {
		private List<DependencyInfo> dependencies;
		private final Set<DependencyInfo> dependenciesSet;

		public DependenciesContentProvider(List<DependencyInfo> dependencies) {
			this.dependencies = dependencies;
			this.dependenciesSet = new HashSet<DependencyInfo>();

			if(dependencies != null) {
				dependenciesSet.addAll(dependencies);
			}
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			if(dependencies != null) {
				return dependencies.toArray();
			}
			return new Object[] {};
		}

		public List<DependencyInfo> getDependencies() {
			if(dependencies == null) {
				dependencies = new ArrayList<DependencyInfo>();
			}
			return dependencies;
		}

		public void addDependency(DependencyInfo dependencyInfo) {
			if(dependencyInfo != null) {
				if(!dependenciesSet.contains(dependencyInfo)) {
					getDependencies().add(dependencyInfo);
					dependenciesSet.add(dependencyInfo);
				}
			}
		}

		public void removeDependencies(List<Integer> dependencyIndices) {
			if(dependencyIndices != null) {
				List<DependencyInfo> dependencies = getDependencies();
				Collections.sort(dependencyIndices);
				int dependencyIndicesCount = dependencyIndices.size();
				for(int i = dependencyIndicesCount - 1; i >= 0; i--) {
					DependencyInfo removedDependencyInfo = dependencies.remove((int)dependencyIndices.get(i));
					dependenciesSet.remove(removedDependencyInfo);
				}
			}
		}

	}

	public DependenciesClasspathContainerPage() {
		super("Dependencies", "Dependencies", null);
		setDescription("Application Dependencies");
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite composite = createDefaultComposite(parent);
		addMainSection(composite);
		setControl(composite);
	}

	@Override
	public void initialize(IJavaProject javaProject, IClasspathEntry[] classpathEntries) {
		this.javaProject = javaProject;
	}

	@Override
	public boolean finish() {
		dependenciesId = UUID.randomUUID().toString();
		DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
		DependenciesStore.getInstance().setDependencies(dependenciesId, dependenciesContentProvider.getDependencies());
		DependenciesStore.getInstance().setResolver(dependenciesId, resolver);

		return true;
	}

	@Override
	public IClasspathEntry getSelection() {
		return JavaCore.newContainerEntry(DependenciesConstants.DEPENDENCIES_CLASSPATH_CONTAINER_ID.append(dependenciesId));
	}

	@Override
	public void setSelection(IClasspathEntry classpathEntry) {
		if(classpathEntry != null) {
			IPath containerPath = classpathEntry.getPath();
			String dependenciesId = containerPath.segment(1);
			if(dependenciesId != null) {
				dependencies = DependenciesStore.getInstance().getDependencies(dependenciesId);
				resolver = DependenciesStore.getInstance().getResolver(dependenciesId);
			}

			IProject project = javaProject.getProject();
			if(dependencies == null) {
				DependenciesParser dependenciesParser = DependenciesParser.getParser(project);
				try {
					dependenciesParser.parse();
					dependencies = DependenciesUtil.copyOf(dependenciesParser.getDependencies());
				}
				catch(CoreException e) {
					DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				catch(IOException e) {
					DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}

			if(resolver == null) {
				resolver = ResolversStore.getResolverForProject(project);
			}
		}

	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);

		return composite;
	}

	private void addMainSection(final Composite parent) {
		final IResource project = javaProject.getProject();

		Label resolverLabel = new Label(parent, SWT.NONE);
		resolverLabel.setText("Dependency Resolver: ");
		FormData fdResolver = new FormData(convertWidthInCharsToPixels(25), convertHeightInCharsToPixels(1));
		fdResolver.left = new FormAttachment(0, 10);
		resolverLabel.setLayoutData(fdResolver);

		final Combo resolversCombo = new Combo(parent, SWT.READ_ONLY);
		FormData fdResolversCombo = new FormData(150, convertHeightInCharsToPixels(1));
		fdResolversCombo.left = new FormAttachment(resolverLabel, 3);
		fdResolversCombo.right = new FormAttachment(100, -5);
		resolversCombo.setLayoutData(fdResolversCombo);

		List<IDependenciesResolver> resolvers = ResolversStore.getResolvers();
		resolversCombo.add(IDependenciesResolver.NULL_RESOLVER.getName());
		resolversCombo.setData(IDependenciesResolver.NULL_RESOLVER.getName(), IDependenciesResolver.NULL_RESOLVER);

		int selectedResolver = 0;
		for(int i = 0; i < resolvers.size(); i++) {
			IDependenciesResolver currentResolver = resolvers.get(i);
			resolversCombo.add(currentResolver.getName());
			resolversCombo.setData(currentResolver.getName(), currentResolver);
			if(resolver != null && resolver.getClass().getName().equals(currentResolver.getClass().getName())) {
				selectedResolver = i + 1;
			}
		}
		resolversCombo.select(selectedResolver);

		Listener resolversComboOnSelectionChangedListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				int selectionIndex = resolversCombo.getSelectionIndex();
				if(selectionIndex >= 0) {
					resolver = (IDependenciesResolver)resolversCombo.getData(resolversCombo.getItem(selectionIndex));
				}

			}

		};

		resolversCombo.addListener(SWT.Selection, resolversComboOnSelectionChangedListener);

		Label resolverSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdResolverSeparatorSeperator = new FormData(10, 10);
		fdResolverSeparatorSeperator.left = new FormAttachment(0, 5);
		fdResolverSeparatorSeperator.right = new FormAttachment(100, -5);
		fdResolverSeparatorSeperator.top = new FormAttachment(resolversCombo, 10, SWT.BOTTOM);
		resolverSeparator.setLayoutData(fdResolverSeparatorSeperator);

		Label dependencyLabel = new Label(parent, SWT.NONE);
		dependencyLabel.setText("Dependency: ");
		FormData fdDependencyLabel = new FormData(convertWidthInCharsToPixels(15), convertHeightInCharsToPixels(1));
		fdDependencyLabel.top = new FormAttachment(resolverSeparator, 3, SWT.BOTTOM);
		fdDependencyLabel.left = new FormAttachment(0, 10);
		dependencyLabel.setLayoutData(fdDependencyLabel);

		final Text dependencyText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		FormData fdDependencyText = new FormData(150, convertHeightInCharsToPixels(1));
		fdDependencyText.top = new FormAttachment(resolverSeparator, 3, SWT.BOTTOM);
		fdDependencyText.left = new FormAttachment(dependencyLabel, 3);
		fdDependencyText.right = new FormAttachment(100, -5);
		dependencyText.setLayoutData(fdDependencyText);

		final Button dependencySelectFileButton = new Button(parent, SWT.PUSH);
		dependencySelectFileButton.setText("Select File...");
		FormData fdDependencySelectFileButton = new FormData(convertWidthInCharsToPixels(25), 20);
		fdDependencySelectFileButton.top = new FormAttachment(dependencyText, 3, SWT.BOTTOM);
		dependencySelectFileButton.setLayoutData(fdDependencySelectFileButton);

		Listener dependencySelectFileButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(parent.getShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String[] {"*.jar"});
				String result = dialog.open();
				if(result != null) {
					dependencyText.setText(result);
				}
			}

		};

		dependencySelectFileButton.addListener(SWT.Selection, dependencySelectFileButtonOnClickEventListener);

		final Button dependencySelectFolderButton = new Button(parent, SWT.PUSH);
		dependencySelectFolderButton.setText("Select Folder...");
		FormData fdDependencySelectFolderButton = new FormData(convertWidthInCharsToPixels(25), 20);
		fdDependencySelectFolderButton.top = new FormAttachment(dependencyText, 3, SWT.BOTTOM);
		dependencySelectFolderButton.setLayoutData(fdDependencySelectFolderButton);

		fdDependencySelectFileButton.right = new FormAttachment(dependencySelectFolderButton, -3);

		Listener dependencySelectFolderButtonOnClickEventListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.OPEN);
				String result = dialog.open();
				if(result != null) {
					dependencyText.setText(result);
				}
			}
		};

		dependencySelectFolderButton.addListener(SWT.Selection, dependencySelectFolderButtonOnClickEventListener);

		final Button addDependencyButton = new Button(parent, SWT.PUSH);
		addDependencyButton.setText("Add");
		FormData fdDependencyAddButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdDependencyAddButton.right = new FormAttachment(100, -5);
		fdDependencyAddButton.top = new FormAttachment(dependencyText, 3, SWT.BOTTOM);
		addDependencyButton.setLayoutData(fdDependencyAddButton);
		addDependencyButton.setEnabled(false);

		fdDependencySelectFolderButton.right = new FormAttachment(addDependencyButton, -3);

		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdSeperator = new FormData(10, 10);
		fdSeperator.left = new FormAttachment(0, 5);
		fdSeperator.right = new FormAttachment(100, -5);
		fdSeperator.top = new FormAttachment(dependencySelectFileButton, 10, SWT.BOTTOM);
		separator.setLayoutData(fdSeperator);

		createDependenciesViewer(parent);
		final Table dependenciesTable = dependenciesViewer.getTable();
		FormData fdDependenciesTable = new FormData(750, 100);
		fdDependenciesTable.top = new FormAttachment(separator, 3, SWT.BOTTOM);
		fdDependenciesTable.left = new FormAttachment(0, 5);
		fdDependenciesTable.right = new FormAttachment(100, -5);
		fdDependenciesTable.bottom = new FormAttachment(100, -35);
		dependenciesTable.setLayoutData(fdDependenciesTable);

		final Button moveDependenciesTopButton = new Button(parent, SWT.PUSH);
		moveDependenciesTopButton.setText("Move Top");
		moveDependenciesTopButton.setEnabled(false);
		FormData fdMoveDependenciesTopButtonButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveDependenciesTopButtonButton.left = new FormAttachment(0, 5);
		fdMoveDependenciesTopButtonButton.top = new FormAttachment(dependenciesTable, 10, SWT.BOTTOM);
		moveDependenciesTopButton.setLayoutData(fdMoveDependenciesTopButtonButton);

		final Button moveDependenciesUpButton = new Button(parent, SWT.PUSH);
		moveDependenciesUpButton.setText("Move Up");
		moveDependenciesUpButton.setEnabled(false);
		FormData fdMoveDependenciesUpButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveDependenciesUpButton.left = new FormAttachment(moveDependenciesTopButton, 5);
		fdMoveDependenciesUpButton.top = new FormAttachment(dependenciesTable, 10, SWT.BOTTOM);
		moveDependenciesUpButton.setLayoutData(fdMoveDependenciesUpButton);

		final Button moveDependenciesDownButton = new Button(parent, SWT.PUSH);
		moveDependenciesDownButton.setText("Move Down");
		moveDependenciesDownButton.setEnabled(false);
		FormData fdMoveDependenciesDownButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveDependenciesDownButton.left = new FormAttachment(moveDependenciesUpButton, 5);
		fdMoveDependenciesDownButton.top = new FormAttachment(dependenciesTable, 10, SWT.BOTTOM);
		moveDependenciesDownButton.setLayoutData(fdMoveDependenciesDownButton);

		final Button moveDependenciesBottomButton = new Button(parent, SWT.PUSH);
		moveDependenciesBottomButton.setText("Move Bottom");
		moveDependenciesBottomButton.setEnabled(false);
		FormData fdMoveDependenciesBottomButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdMoveDependenciesBottomButton.left = new FormAttachment(moveDependenciesDownButton, 5);
		fdMoveDependenciesBottomButton.top = new FormAttachment(dependenciesTable, 10, SWT.BOTTOM);
		moveDependenciesBottomButton.setLayoutData(fdMoveDependenciesBottomButton);

		final Button removeDependenciesButton = new Button(parent, SWT.PUSH);
		removeDependenciesButton.setText("Remove");
		FormData fdRemoveDependenciesButton = new FormData(convertWidthInCharsToPixels(15), 20);
		fdRemoveDependenciesButton.left = new FormAttachment(moveDependenciesBottomButton, 5);
		fdRemoveDependenciesButton.top = new FormAttachment(dependenciesTable, 10, SWT.BOTTOM);
		removeDependenciesButton.setLayoutData(fdRemoveDependenciesButton);
		removeDependenciesButton.setEnabled(false);

		final Button[] moveDependenciesButtons = new Button[] {moveDependenciesTopButton, moveDependenciesUpButton, moveDependenciesDownButton, moveDependenciesBottomButton};
		Listener moveDependenciesTopButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = dependenciesTable.getSelectionIndices();
				if(selectedIndices != null) {
					DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
					List<DependencyInfo> dependencies = dependenciesContentProvider.getDependencies();

					Set<DependencyInfo> checkedDependencies = getCheckedDependencies(dependenciesTable, dependencies);

					Arrays.sort(selectedIndices);

					List<DependencyInfo> removedDependencies = new ArrayList<DependencyInfo>();
					for(int i = selectedIndices.length - 1; i >= 0; i--) {
						removedDependencies.add(0, dependencies.remove(selectedIndices[i]));
					}
					dependencies.addAll(0, removedDependencies);

					dependenciesViewer.refresh();

					checkItems(dependenciesTable, dependencies, checkedDependencies);

					setMoveDependenciesButtonsEnablement(dependenciesTable.getSelectionIndices(), dependenciesTable.getItemCount(), moveDependenciesButtons);
				}
			}

		};

		moveDependenciesTopButton.addListener(SWT.Selection, moveDependenciesTopButtonOnClickEventListener);

		Listener moveDependenciesUpButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = dependenciesTable.getSelectionIndices();
				if(selectedIndices != null) {
					DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
					List<DependencyInfo> dependencies = dependenciesContentProvider.getDependencies();

					Set<DependencyInfo> checkedDependencies = getCheckedDependencies(dependenciesTable, dependencies);

					Arrays.sort(selectedIndices);

					for(int i = 0; i < selectedIndices.length; i++) {
						int currentIndex = selectedIndices[i];
						int previousIndex = (currentIndex - 1);
						if(previousIndex >= 0) {
							DependencyInfo previousDependency = dependencies.get(previousIndex);
							DependencyInfo currentDependency = dependencies.get(currentIndex);
							dependencies.set(previousIndex, currentDependency);
							dependencies.set(currentIndex, previousDependency);
						}
					}

					dependenciesViewer.refresh();

					checkItems(dependenciesTable, dependencies, checkedDependencies);

					setMoveDependenciesButtonsEnablement(dependenciesTable.getSelectionIndices(), dependenciesTable.getItemCount(), moveDependenciesButtons);
				}
			}

		};

		moveDependenciesUpButton.addListener(SWT.Selection, moveDependenciesUpButtonOnClickEventListener);

		Listener moveDependenciesDownButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = dependenciesTable.getSelectionIndices();
				if(selectedIndices != null) {
					DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
					List<DependencyInfo> dependencies = dependenciesContentProvider.getDependencies();

					Set<DependencyInfo> checkedDependencies = getCheckedDependencies(dependenciesTable, dependencies);

					Arrays.sort(selectedIndices);

					int itemCount = dependenciesTable.getItemCount();

					for(int i = selectedIndices.length - 1; i >= 0; i--) {
						int currentIndex = selectedIndices[i];
						int nextIndex = (currentIndex + 1);
						if(nextIndex < itemCount) {
							DependencyInfo previousDependency = dependencies.get(nextIndex);
							DependencyInfo currentDependency = dependencies.get(currentIndex);
							dependencies.set(nextIndex, currentDependency);
							dependencies.set(currentIndex, previousDependency);
						}
					}

					dependenciesViewer.refresh();

					checkItems(dependenciesTable, dependencies, checkedDependencies);

					setMoveDependenciesButtonsEnablement(dependenciesTable.getSelectionIndices(), itemCount, moveDependenciesButtons);
				}
			}

		};

		moveDependenciesDownButton.addListener(SWT.Selection, moveDependenciesDownButtonOnClickEventListener);

		Listener moveDependenciesBottomButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				int[] selectedIndices = dependenciesTable.getSelectionIndices();
				if(selectedIndices != null) {

					DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
					List<DependencyInfo> dependencies = dependenciesContentProvider.getDependencies();

					Set<DependencyInfo> checkedDependencies = getCheckedDependencies(dependenciesTable, dependencies);

					Arrays.sort(selectedIndices);

					List<DependencyInfo> removedDependencies = new ArrayList<DependencyInfo>();
					for(int i = selectedIndices.length - 1; i >= 0; i--) {
						removedDependencies.add(0, dependencies.remove(selectedIndices[i]));
					}
					dependencies.addAll(removedDependencies);

					dependenciesViewer.refresh();

					checkItems(dependenciesTable, dependencies, checkedDependencies);

					setMoveDependenciesButtonsEnablement(dependenciesTable.getSelectionIndices(), dependenciesTable.getItemCount(), moveDependenciesButtons);
				}
			}

		};

		moveDependenciesBottomButton.addListener(SWT.Selection, moveDependenciesBottomButtonOnClickEventListener);

		dependencyText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				addDependencyButton.setEnabled(false);

				String dependencyStr = dependencyText.getText();
				String resolvedPath = DependenciesUtil.resolvePath(project, dependencyStr);
				if(resolvedPath != null) {
					dependencyStr = resolvedPath;
					dependencyText.setData(dependencyStr);
					addDependencyButton.setEnabled(true);
				}
			}
		});

		Listener addDependencyButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
				DependencyInfo dependencyInfo = new DependencyInfo(dependencyText.getText(), (String)dependencyText.getData(), DependenciesConstants.ASSOCIATED_PROJECT_NONE, DependenciesConstants.DEPENDENCY_SCOPE_COMPILE);
				dependenciesContentProvider.addDependency(dependencyInfo);
				dependenciesViewer.refresh();

			}
		};

		addDependencyButton.addListener(SWT.Selection, addDependencyButtonOnClickEventListener);

		Listener removeDependenciesButtonButtonOnClickEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				DependenciesContentProvider dependenciesContentProvider = (DependenciesContentProvider)dependenciesViewer.getContentProvider();
				List<Integer> checkedItemIndices = new ArrayList<Integer>();

				TableItem[] items = dependenciesTable.getItems();
				if(items != null) {
					for(int i = 0; i < items.length; i++) {
						if(items[i].getChecked()) {
							checkedItemIndices.add(i);
						}
					}
				}

				if(!checkedItemIndices.isEmpty()) {
					dependenciesContentProvider.removeDependencies(checkedItemIndices);
					dependenciesViewer.refresh();

					removeDependenciesButton.setEnabled(false);
				}
			}
		};

		removeDependenciesButton.addListener(SWT.Selection, removeDependenciesButtonButtonOnClickEventListener);

		Listener dependenciesTableOnSelectEventListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				setMoveDependenciesButtonsEnablement(dependenciesTable.getSelectionIndices(), dependenciesTable.getItemCount(), moveDependenciesButtons);

				removeDependenciesButton.setEnabled(isAnyItemChecked(dependenciesTable));
			}

		};

		dependenciesTable.addListener(SWT.Selection, dependenciesTableOnSelectEventListener);
	}

	private void setMoveDependenciesButtonsEnablement(int[] selectedIndices, int itemCount, final Button[] moveDependenciesButtons) {
		if(allowSelectionToMoveUp(selectedIndices)) {
			moveDependenciesButtons[0].setEnabled(true);
			moveDependenciesButtons[1].setEnabled(true);
		}
		else {
			moveDependenciesButtons[0].setEnabled(false);
			moveDependenciesButtons[1].setEnabled(false);
		}

		if(allowSelectionToMoveDown(selectedIndices, itemCount)) {
			moveDependenciesButtons[2].setEnabled(true);
			moveDependenciesButtons[3].setEnabled(true);
		}
		else {
			moveDependenciesButtons[2].setEnabled(false);
			moveDependenciesButtons[3].setEnabled(false);
		}
	}

	private boolean allowSelectionToMoveUp(int[] selectedIndices) {
		boolean result = false;
		Arrays.sort(selectedIndices);
		for(int i = 0; i < selectedIndices.length; i++) {
			if(selectedIndices[i] != i) {
				result = true;
			}
		}
		return result;
	}

	private boolean allowSelectionToMoveDown(int[] selectedIndices, int itemCount) {
		boolean result = false;
		Arrays.sort(selectedIndices);
		for(int i = selectedIndices.length - 1, j = 1; i >= 0; i--, j++) {
			if(selectedIndices[i] != itemCount - j) {
				result = true;
			}
		}
		return result;
	}

	private boolean isAnyItemChecked(final Table dependenciesTable) {
		boolean isAnyItemChecked = false;
		TableItem[] items = dependenciesTable.getItems();
		if(items != null) {
			for(int i = 0; i < items.length; i++) {
				if(items[i].getChecked()) {
					isAnyItemChecked = true;
					break;
				}
			}
		}
		return isAnyItemChecked;
	}

	private List<Integer> getCheckedItemIndices(final Table dependenciesTable) {
		List<Integer> result = new ArrayList<Integer>();
		TableItem[] items = dependenciesTable.getItems();
		if(items != null) {
			for(int i = 0; i < items.length; i++) {
				if(items[i].getChecked()) {
					result.add(i);
				}
			}
		}
		return result;
	}

	private Set<DependencyInfo> getCheckedDependencies(final Table dependenciesTable, List<DependencyInfo> dependencies) {
		List<Integer> checkedItemIndices = getCheckedItemIndices(dependenciesTable);
		Set<DependencyInfo> result = new HashSet<DependencyInfo>();
		for(int checkedItemIndex: checkedItemIndices) {
			result.add(dependencies.get(checkedItemIndex));
		}

		return result;
	}

	private void checkItems(final Table dependenciesTable, List<DependencyInfo> dependencies, Set<DependencyInfo> checkedDependencies) {
		TableItem[] items = dependenciesTable.getItems();
		if(items != null) {
			for(int i = 0; i < items.length; i++) {
				if(checkedDependencies.contains(dependencies.get(i))) {
					items[i].setChecked(true);
				}
				else {
					items[i].setChecked(false);
				}
			}
		}
	}

	private TableViewer createDependenciesViewer(Composite parent) {

		dependenciesViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		DependenciesContentProvider dependenciesContentProvider = new DependenciesContentProvider(dependencies);
		dependenciesViewer.setContentProvider(dependenciesContentProvider);

		final Table dependencyTable = dependenciesViewer.getTable();
		dependencyTable.setHeaderVisible(true);
		dependencyTable.setLinesVisible(true);

		TableViewerColumn dependencyViewerColumn1 = new TableViewerColumn(dependenciesViewer, SWT.NONE);
		dependencyViewerColumn1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					DependencyInfo dependencyInfo = (DependencyInfo)element;
					return dependencyInfo.getPath();
				}
				return null;
			}
		});

		dependencyViewerColumn1.setEditingSupport(new DependencyPathEditingSupport(dependenciesViewer, javaProject));

		TableColumn dependencyTableColumn1 = dependencyViewerColumn1.getColumn();
		dependencyTableColumn1.setText("Dependency Path");
		dependencyTableColumn1.setWidth(490);
		dependencyTableColumn1.setResizable(true);

		TableViewerColumn dependencyViewerColumn2 = new TableViewerColumn(dependenciesViewer, SWT.NONE);
		dependencyViewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					DependencyInfo dependencyInfo = (DependencyInfo)element;
					return dependencyInfo.getAssociatedProject();
				}
				return null;
			}
		});

		dependencyViewerColumn2.setEditingSupport(new DependencyAssociatedProjectEditingSupport(dependenciesViewer, javaProject));

		TableColumn dependencyTableColumn2 = dependencyViewerColumn2.getColumn();
		dependencyTableColumn2.setText("Associated Project");
		dependencyTableColumn2.setWidth(150);
		dependencyTableColumn2.setResizable(true);

		TableViewerColumn dependencyViewerColumn3 = new TableViewerColumn(dependenciesViewer, SWT.NONE);
		dependencyViewerColumn3.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if(element != null) {
					DependencyInfo dependencyInfo = (DependencyInfo)element;
					return DependencyInfo.getScopeLabel(dependencyInfo.getScope());
				}
				return null;
			}
		});

		dependencyViewerColumn3.setEditingSupport(new DependencyScopeEditingSupport(dependenciesViewer));

		TableColumn dependencyTableColumn3 = dependencyViewerColumn3.getColumn();
		dependencyTableColumn3.setText("Dependency Scope");
		dependencyTableColumn3.setWidth(110);
		dependencyTableColumn3.setResizable(true);

		dependenciesViewer.setInput(dependenciesContentProvider.getDependencies());

		return dependenciesViewer;
	}
}
