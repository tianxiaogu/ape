package com.android.commands.monkey.ape.naming;

import java.util.EnumSet;

import org.w3c.dom.Element;

import com.android.commands.monkey.ape.tree.GUITreeBuilder;
import com.android.commands.monkey.ape.tree.GUITreeNode;

public class TextNamer extends AbstractNamer implements SingletonNamer {

    class TextName extends AbstractLocalName {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        String text;
        String contentDesc;

        public TextName(String text, String contentDesc) {
            super();
            this.text = text;
            this.contentDesc = contentDesc;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((contentDesc == null) ? 0 : contentDesc.hashCode());
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TextName other = (TextName) obj;
            if (contentDesc == null) {
                if (other.contentDesc != null)
                    return false;
            } else if (!contentDesc.equals(other.contentDesc))
                return false;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }

        @Override
        public Namer getNamer() {
            return TextNamer.this;
        }

        public String toString() {
            if (contentDesc == null || contentDesc.length() == 0) {
                return "text=" + text + ";";
            }
            return "text=" + text + ";content-desc=" + contentDesc + ";";
        }

        @Override
        public Name getLocalName() {
            return this;
        }

        @Override
        public void appendXPathLocalProperties(StringBuilder sb) {
            sb.append("[@text=\"");
            sb.append(NamerFactory.escapeToXPathString(text));
            sb.append("\"][@content-desc=\"");
            sb.append(NamerFactory.escapeToXPathString(contentDesc));
            sb.append("\"]");
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TextNamer() {
        super(EnumSet.of(NamerType.TEXT));
    }

    protected String getAttributeValue(Element e, String prop) {
        if (!"text".equals(prop)) {
            return e.getAttribute(prop);
        }
        String cls = e.getAttribute("class");
        if (GUITreeBuilder.isEditText(cls)) {
            return "";
        }
        return e.getAttribute(prop);
    }

    @Override
    public NamerType getNamerType() {
        return NamerType.TEXT;
    }

    @Override
    public Name naming(GUITreeNode node) {
        return NameManager.getCachedName(new TextName(node.getText(), node.getContentDesc()));
    }

    public String toString() {
        return "TextNamer[text,content-desc]";
    }
}
