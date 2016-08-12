package com.cwctravel.eclipse.plugins.dependencies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class DependenciesParser implements ContentHandler {
	private static final int STATE_DOCUMENT_START = 0;
	private static final int STATE_DEPENDENCIES_TAG_START = 1;
	private static final int STATE_DEPENDENCY_TAG_START = 2;
	private static final int STATE_IGNORED = 3;

	private static final Map<IProject, DependenciesParser> parserMap = Collections.synchronizedMap(new WeakHashMap<IProject, DependenciesParser>());

	private final IProject project;
	private final IFile dependenciesFile;
	private final IPathVariableManager pathVariableManager;
	private final ILaunchConfiguration launchConfiguration;
	private final Lock processingLock = new ReentrantLock();

	private Deque<Integer> stateStack;
	private List<DependencyInfo> dependenciesList;

	private long modificationTimeStamp;
	private long preferencesModificationTimeStamp;

	private DependenciesParser(IProject project) {
		this.project = project;
		this.dependenciesFile = null;
		this.pathVariableManager = null;
		this.launchConfiguration = null;
	}

	public DependenciesParser(IFile dependenciesFile, IPathVariableManager pathVariableManager) {
		this.project = null;
		this.launchConfiguration = null;
		this.dependenciesFile = dependenciesFile;
		this.pathVariableManager = pathVariableManager;
	}

	public DependenciesParser(ILaunchConfiguration launchConfiguration) throws CoreException {
		this.launchConfiguration = launchConfiguration;
		this.project = null;
		this.dependenciesFile = null;
		this.pathVariableManager = null;
	}

	private IFile getDependenciesFile() throws CoreException {
		if(dependenciesFile != null) {
			return dependenciesFile;
		}
		else if(project != null) {
			return project.getFile(Path.fromPortableString("dependencies.xml"));
		}
		else if(launchConfiguration != null) {
			String dependenciesPath = launchConfiguration.getAttribute(DependenciesConstants.DEPENDENCIES_PATH_ATTR, (String)null);
			String launchingProjectName = launchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
			IProject launchingProject = ResourcesPlugin.getWorkspace().getRoot().getProject(launchingProjectName);
			return launchingProject.getFile(new Path(dependenciesPath));
		}
		return null;
	}

	private IPathVariableManager getPathVariableManager() throws CoreException {
		if(pathVariableManager != null) {
			return pathVariableManager;
		}
		else if(project != null) {
			return project.getPathVariableManager();
		}
		else if(launchConfiguration != null) {
			String launchingProjectName = launchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
			IProject launchingProject = ResourcesPlugin.getWorkspace().getRoot().getProject(launchingProjectName);
			return launchingProject.getPathVariableManager();
		}
		return null;
	}

	private String getSource() {
		if(project != null) {
			return project.getName();
		}
		else if(launchConfiguration != null) {
			return launchConfiguration.getName();
		}
		else if(dependenciesFile != null) {
			return dependenciesFile.getName();
		}

		return "<unknown source>";
	}

	public void parse() throws CoreException, IOException {
		IFile dependenciesFile = getDependenciesFile();
		IPathVariableManager pathVariableManager = getPathVariableManager();
		if(dependenciesFile != null && dependenciesFile.exists()) {
			processingLock.lock();
			try {
				long currentPreferencesModificationTimeStamp = 0;

				if(project != null) {
					File preferencesLocation = new ProjectScope(project).getLocation().toFile();
					File preferencesFile = new File(preferencesLocation, DependenciesConstants.DEPENDENCIES_PLUGIN_ID + ".prefs");
					currentPreferencesModificationTimeStamp = preferencesFile.isFile() ? preferencesFile.lastModified() : 0L;
				}

				if(dependenciesList == null || modificationTimeStamp == 0 || (project != null && preferencesModificationTimeStamp == 0) || dependenciesFile.getModificationStamp() != modificationTimeStamp || preferencesModificationTimeStamp != currentPreferencesModificationTimeStamp) {
					InputStream contents = dependenciesFile.getContents(true);
					try {
						parse(contents, pathVariableManager);
						modificationTimeStamp = dependenciesFile.getModificationStamp();
						preferencesModificationTimeStamp = currentPreferencesModificationTimeStamp;
					}
					finally {
						contents.close();
					}
				}
			}
			finally {
				processingLock.unlock();
			}
		}
	}

	private void parse(InputStream dependenciesInputStream, IPathVariableManager pathVariableManager) {
		this.stateStack = new ArrayDeque<Integer>();
		this.dependenciesList = new ArrayList<DependencyInfo>();
		Reader reader = null;
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(false);
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(this);
			reader = new InputStreamReader(dependenciesInputStream);
			DependenciesBuilderPlugin.log(IStatus.INFO, "Parsing dependencies file for " + getSource(), null);
			xmlReader.parse(new InputSource(reader));
		}
		catch(IOException e) {
			DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
		catch(SAXException e) {
			DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
		catch(ParserConfigurationException e) {
			DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}
		finally {
			try {
				if(reader != null) {
					reader.close();
				}
			}
			catch(IOException e) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
	}

	@Override
	public void characters(char[] arg0, int arg1, int arg2) throws SAXException {

	}

	@Override
	public void endDocument() throws SAXException {
		stateStack.pop();
	}

	@Override
	public void endElement(String arg0, String arg1, String arg2) throws SAXException {
		stateStack.pop();
	}

	@Override
	public void endPrefixMapping(String arg0) throws SAXException {

	}

	@Override
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {

	}

	@Override
	public void processingInstruction(String arg0, String arg1) throws SAXException {

	}

	@Override
	public void setDocumentLocator(Locator arg0) {

	}

	@Override
	public void skippedEntity(String arg0) throws SAXException {

	}

	@Override
	public void startDocument() throws SAXException {
		stateStack.push(STATE_DOCUMENT_START);

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		Integer currentState = stateStack.peek();
		try {
			if(currentState == STATE_DOCUMENT_START && "dependencies".equals(qName)) {
				stateStack.push(STATE_DEPENDENCIES_TAG_START);
			}
			else if(currentState == STATE_DEPENDENCIES_TAG_START && "dependency".equals(qName)) {
				stateStack.push(STATE_DEPENDENCY_TAG_START);
				String rawPath = atts.getValue("path");
				String path = rawPath;
				IPathVariableManager pathVariableManager = getPathVariableManager();
				if(pathVariableManager != null) {
					path = URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(path))).toOSString();
				}

				String scopeLabel = atts.getValue("scope");
				int dependencyScope = DependencyInfo.getScope(scopeLabel);

				String associatedProject = DependenciesConstants.ASSOCIATED_PROJECT_NONE;
				if(project != null) {
					IEclipsePreferences preferences = new ProjectScope(project).getNode(DependenciesConstants.DEPENDENCIES_PLUGIN_ID);
					associatedProject = preferences.get(DependenciesConstants.DEPENDENCY_ASSOCIATED_PROJECT_PROPERTY_PREFIX + rawPath, DependenciesConstants.ASSOCIATED_PROJECT_NONE);
				}
				else if(launchConfiguration != null) {
					associatedProject = launchConfiguration.getAttribute(DependenciesConstants.DEPENDENCY_ASSOCIATED_PROJECT_PROPERTY_PREFIX + rawPath, DependenciesConstants.ASSOCIATED_PROJECT_NONE);
				}

				DependencyInfo dependencyInfo = new DependencyInfo(rawPath, path, associatedProject, dependencyScope);
				dependenciesList.add(dependencyInfo);
			}
			else {
				stateStack.push(STATE_IGNORED);
			}
		}
		catch(CoreException cE) {
			throw new SAXException(cE);
		}
	}

	@Override
	public void startPrefixMapping(String arg0, String arg1) throws SAXException {

	}

	public void reset() {
		modificationTimeStamp = 0;
		preferencesModificationTimeStamp = 0;
	}

	public List<DependencyInfo> getDependencies() {
		return dependenciesList;
	}

	public static synchronized void removeParser(IProject project) {
		if(project != null) {
			parserMap.remove(project);
		}
	}

	public static synchronized DependenciesParser getParser(IProject project) {
		DependenciesParser result = null;
		if(project != null) {
			result = parserMap.get(project);
			if(result == null) {
				result = new DependenciesParser(project);
				parserMap.put(project, result);

			}
		}

		return result;
	}

}
