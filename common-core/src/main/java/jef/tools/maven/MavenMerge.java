package jef.tools.maven;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Element;

import jef.tools.IOUtils;
import jef.tools.maven.jaxb.Dependency;
import jef.tools.maven.jaxb.Model;
import jef.tools.maven.jaxb.Model.Dependencies;

/**
 * 
 * 当switch profile时, 需要把原工程的pom.xml文件跟新的profile里的pom.xml配置整合.<br>
 * 策略如下: <br>
 * <ul>1 整合properties节点</ul>
 * <li> 如果tagName在两个pom中都存在, 新的profile里的内容直接替换原工程pom中的内容<li>
 * <li>如果tagName在原工程的pom中不存在, 拷贝内容到原工程的pom</li>
 * <li> 如果tagName在新的pom中不存在, 原工程pom节点不变化
 * <ul>2 整合dependency节点</ul>
 * <li> 如果artifactId, groupId在两个pom中都存在, 新的profile里的内容直接替换原工程pom中的相应dependency节点<li>
 * <li>如果artifactId, groupId在原工程的pom中不存在, 拷贝内容到原工程的pom的dependency节点</li>
 * <li>如果artifactId, groupId在新的pom中不存在, 原工程pom dependency节点不变化
 * 
 * @author Jinrm
 *
 */
public class MavenMerge {

	public void merge(File oldPom, File newPom) throws JAXBException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException{
		if (!oldPom.exists()){
			IOUtils.copyFile(newPom, oldPom);
			return;
		}
		JAXBContext cxt = JAXBContext.newInstance("jef.tools.maven.jaxb");
		Unmarshaller unm = cxt.createUnmarshaller();
		JAXBElement<Model> oldEle=(JAXBElement<Model>)unm.unmarshal(oldPom);
		JAXBElement<Model> newEle=(JAXBElement<Model>)unm.unmarshal(newPom);	
		Model oldModel=oldEle.getValue();
		Model newModel=newEle.getValue();
		
		if (oldModel.getProperties()==null){
			oldModel.setProperties(newModel.getProperties());
		}else if (oldModel.getProperties()!=null && newModel.getProfiles()!=null){
			mergeProperty(oldModel.getProperties(), newModel.getProperties());
		}
		
		if (oldModel.getDependencies()==null){
			oldModel.setDependencies(newModel.getDependencies());
		}else if (oldModel.getDependencies()!=null && newModel.getDependencies()!=null){
			mergeDependency(oldModel.getDependencies(), newModel.getDependencies());
		}

		Marshaller marshaller=cxt.createMarshaller();
		marshaller.marshal(oldEle, oldPom);
	}
	
	private void mergeProperty(Model.Properties oldP, Model.Properties newP){
		List<Element> oldList=oldP.getAny();
		List<Element> newList=newP.getAny();
		for (Element ele:newList){
			Element foundEle=findPropertyInList(ele, oldList);
			if (foundEle==null)
				oldList.add(ele);
			else
				mergeProperty(foundEle, ele);
		}
	}
	
	private void mergeProperty(Element oldE, Element newE){
		String destVal=newE.getTextContent();
		oldE.setTextContent(destVal);
	}
	
	private Element findPropertyInList(Element ele, List<Element> list){
		String tagName=ele.getTagName();
		for (Element anE:list){
			if (anE.getTagName().equals(tagName)){
				return anE;
			}
		}
		return null;
	}
	
	private void mergeDependency(Dependencies oldD, Dependencies newD){
		List<Dependency> newDs=newD.getDependency();
		List<Dependency> oldDs=oldD.getDependency();
		for (Dependency aNewD: newDs){
			Dependency foundD=findDependInList(aNewD, oldDs);
			if (foundD==null){
				oldDs.add(aNewD);
			}else{
				mergeDependency(foundD, aNewD);
			}
		}
	}
	
	private void mergeDependency(Dependency oldD, Dependency newD){
		String oldVersion=oldD.getVersion().trim();
		if (oldVersion!=null && !oldVersion.startsWith("${")){
			oldD.setArtifactId(newD.getArtifactId());
			oldD.setClassifier(newD.getClassifier());
			oldD.setGroupId(newD.getGroupId());
			oldD.setExclusions(newD.getExclusions());
			oldD.setOptional(newD.isOptional());
			oldD.setScope(newD.getScope());
			oldD.setSystemPath(newD.getSystemPath());
			oldD.setType(newD.getType());
			oldD.setVersion(newD.getVersion());
		}
	}
	
	private Dependency findDependInList(Dependency depend, List<Dependency> list){
		String artifactId=depend.getArtifactId();
		String groupId=depend.getGroupId();
		
		for (Dependency aDepend:list){
			if (artifactId.equals(aDepend.getArtifactId()) && groupId.equals(aDepend.getGroupId())){
				return aDepend;
			}
		}
		return null;
	}
	
	
	
	public static void main(String[] args){
		
		MavenMerge compare=new MavenMerge();
		try {
			String oldFile="E:\\workspace\\testC\\pom.xml";
			String newFile="E:\\workspace\\easyframe\\plug-ins\\easybuilder\\plugin_conf\\template\\ProjRoot\\pom.xml";
			compare.merge(new File(oldFile), new File(newFile));
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
