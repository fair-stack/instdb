package cn.cnic.instdb.utils;

import cn.cnic.instdb.model.system.Template;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/14:34
 * @Description:
 */

@Slf4j
public class XmlTemplateUtil {

    private static final String URL = "src/main/resources/templates/";

    public static void main(String[] args) {
        File file = new File(URL+"Psychological Institute Template Design.xml");
        Template template = getTemplate(file);
        System.out.println(template.getTemplateName());
    }

    /**
     * Obtain information from the template
     *
     * @param templatePath
     * @return
     */
    public static Template getTemplateInfo(String templatePath) {
        File file = new File(templatePath);
        if (!file.exists() || file.length() == 0) {
            throw new RuntimeException(templatePath + " The content of the metadata standard file is empty");
        }
        return getTemplate(file);
    }

    //Parsing templates
    public static String getTemplateStr(String templatePath) {
        File file = new File(templatePath);
        if (!file.exists() || file.length() == 0) {
            throw new RuntimeException(templatePath + " The content of the metadata standard file is empty");
        }
        String result = "";
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(file);
            result = document.asXML();
        } catch (Exception e) {
            log.error("analysisxmlanalysis" + e);
            throw new RuntimeException("Abnormal parsing of metadata standard template file");
        }
        return result;
    }


    /**
     * //The second step of obtaining a template  The second step of obtaining a template
     *
     * @param bookstore
     * @param template
     */
    private static List getTemplateBasicInfo(Element bookstore, Template template) {
        //Start traversing the second layer  Start traversing the second layer
        Iterator storeit = bookstore.elementIterator();
        //Start loop
        while (storeit.hasNext()) {
            Element bookElement = (Element) storeit.next();

            //Get fixedname Get fixed
            Attribute name1 = bookElement.attribute("name");
            //according tonameaccording to according tovalue
            String value1 = name1.getValue();
            //Get fixedname Get fixed
            Attribute value = bookElement.attribute("value");

            //according tonameaccording to according tovalue
            if ("name".equals(value1)) {
                template.setTemplateName(value.getValue());
            } else if ("desc".equals(value1)) {
                template.setTemplateDesc(value.getValue());
            } else if ("version".equals(value1)) {
                template.setVersion(value.getValue());
            } else if ("author".equals(value1)) {
                template.setTemplateAuthor(value.getValue());
            } else if ("root".equals(value1)) {
                return bookElement.elements("group");
            }
        }
        log.info("Successfully obtained the basic information of the template..." + template.getTemplateAuthor());
        return null;
    }


    //Parsing templates
    public static Template getTemplate(File file) {
        Template template = new Template();
        SAXReader reader = new SAXReader();

        try {
            Document document = reader.read(file);
            Element element = document.getRootElement();

            List<Template.Group> groupList = new ArrayList<>();
            //Obtain basic information about templates
            List<Element> templateBasicInfo = getTemplateBasicInfo(element, template);
            for (Element elemens : templateBasicInfo) {
                Template.Group groupObj = new Template.Group();
                //Obtain fixed group attribute names
                Attribute value = elemens.attribute("value");
                groupObj.setName(value.getValue());
                //Obtain fixed group attribute descriptions
                Attribute desc = elemens.attribute("desc");
                groupObj.setDesc(desc.getValue());
                List<Template.Resource> listResource = new ArrayList<>();
                //Start processing the items in the grouplist
                List<Element> lists = elemens.elements("list");
                for (Element list : lists) {
                    List<Element> beans = list.elements("bean");
                    //Start processingbeanStart processing
                    for (Element bean : beans) {
                        List<Element> elements = bean.elements();
                        Template.Resource resource = new Template.Resource();
                        for (Element elementBean : elements) {
                            //startsetstart
                            setAttribute(resource, elementBean);
                        }
                        //Place each attribute content in thelistPlace each attribute content in the
                        listResource.add(resource);
                    }
                    //Place the attribute sets within each group in the group
                    groupObj.setResources(listResource);
                }
                //holdgroupholdlisthold  holdgroup listhold
                groupList.add(groupObj);
                template.setGroup(groupList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("analysisxmlanalysis" + e);
            throw new RuntimeException("Abnormal parsing of metadata standard template file");
        }
        return template;
    }


    /**
     * Set the attributes in theOptions
     * @param element
     * @return
     */
    private static List<Template.Options> setOptions(Element element) {
        Iterator listIterator = element.elementIterator();
        List<Template.Options> listOptions = new ArrayList<>();
        while (listIterator.hasNext()) {
            Element options = (Element) listIterator.next();
            List<Element> optionElements = options.elements();
            Template.Options optionObj = new Template.Options();
            for (Element optionElement : optionElements) {
                //Get fixedname Get fixed
                Attribute name1 = optionElement.attribute("name");
                //according tonameaccording to according tovalue
                String nameValue1 = name1.getValue();
                //Get fixedname Get fixed
                Attribute value1 = optionElement.attribute("value");
                //according tonameaccording to according tovalue
                if ("name".equals(nameValue1)) {
                    optionObj.setName(value1.getValue());
                } else if ("title".equals(nameValue1)) {
                    optionObj.setTitle(value1.getValue());
                } else if ("type".equals(nameValue1)) {
                    optionObj.setType(value1.getValue());
                }else if ("formate".equals(nameValue1)) {
                    optionObj.setFormate(value1.getValue());
                } else if ("url".equals(nameValue1)) {
                    optionObj.setUrl(value1.getValue());
                } else if ("placeholder".equals(nameValue1)) {
                    optionObj.setPlaceholder(value1.getValue());
                }else if ("mode".equals(nameValue1)) {
                    optionObj.setMode(value1.getValue());
                }else if ("children".equals(nameValue1)) {
                    //To handleoptionTo handle
                    Element childrenElement = optionElement.element("list");
                    //Return the content of children
                    List<Template.Children> children = setChildren(childrenElement);
                    optionObj.setChildren(children);
                }
            }
            listOptions.add(optionObj);
        }
        return listOptions;
    }

    /**
     * Set the attributes in theChildren
     * @param element
     * @return
     */
    private static List<Template.Children> setChildren(Element element) {
        Iterator childrenIterator = element.elementIterator();
        List<Template.Children> listChildren = new ArrayList<>();
        while (childrenIterator.hasNext()) {
            Element Elementchildren = (Element) childrenIterator.next();
            List<Element> childrenList = Elementchildren.elements();
            Template.Children childrenObj = new Template.Children();
            for (Element children : childrenList) {
                //Get fixedname Get fixed
                Attribute childrenName = children.attribute("name");
                //according tonameaccording to according tovalue
                String childrenValueStr = childrenName.getValue();
                //Get fixedname Get fixed
                Attribute childrenValue = children.attribute("value");
                //according tonameaccording to according tovalue
                if ("name".equals(childrenValueStr)) {
                    childrenObj.setName(childrenValue.getValue());
                } else if ("title".equals(childrenValueStr)) {
                    childrenObj.setTitle(childrenValue.getValue());
                } else if ("type".equals(childrenValueStr)) {
                    childrenObj.setType(childrenValue.getValue());
                }else if ("formate".equals(childrenValueStr)) {
                    childrenObj.setFormate(childrenValue.getValue());
                }  else if ("placeholder".equals(childrenValueStr)) {
                    childrenObj.setPlaceholder(childrenValue.getValue());
                }else if ("options".equals(childrenValueStr)) {
                    Element options = children.element("list");
                    //If there isoptions If there is
                    List<Template.Options> optionsList = setOptions(options);
                    childrenObj.setOptions(optionsList);
                }
            }
            listChildren.add(childrenObj);
        }
        return listChildren;
    }

    /**
     * Set Property Content
     * @param resource
     * @param element
     */
    private static void setAttribute(Template.Resource resource, Element element) {

        Attribute nameValues = element.attribute("name");
        String nameValue = nameValues.getValue();
        Attribute beanValue = element.attribute("value");
        if ("name".equals(nameValue)) {
            resource.setName(beanValue.getValue());
        } else if ("title".equals(nameValue)) {
            resource.setTitle(beanValue.getValue());
        } else if ("type".equals(nameValue)) {
            resource.setType(beanValue.getValue());
        } else if ("check".equals(nameValue)) {
            resource.setCheck(beanValue.getValue());
        } else if ("multiply".equals(nameValue)) {
            resource.setMultiply(beanValue.getValue());
        } else if ("placeholder".equals(nameValue)) {
            resource.setPlaceholder(beanValue.getValue());
        } else if ("iri".equals(nameValue)) {
            resource.setIri(beanValue.getValue());
        } else if ("language".equals(nameValue)) {
            resource.setLanguage(beanValue.getValue());
        }  else if ("formate".equals(nameValue)) {
            resource.setFormate(beanValue.getValue());
        } else if ("mode".equals(nameValue)) {
            resource.setMode(beanValue.getValue());
        }else if ("options".equals(nameValue)) {
            //handleoptions
            Element options = element.element("list");
            List<Template.Options> optionsList = setOptions(options);
            resource.setOptions(optionsList);
        }else if ("show".equals(nameValue)) {
            //handleoptions
            Element options = element.element("list");
            List<Template.Options> optionsList = setOptions(options);
            resource.setShow(optionsList);
        }else if ("operation".equals(nameValue)) {
            //handleoptions
            Element options = element.element("list");
            List<Template.Options> optionsList = setOptions(options);
            resource.setOperation(optionsList);
        }
    }
}
