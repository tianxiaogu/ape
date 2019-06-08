package com.android.commands.monkey.ape.tree;

import java.io.Serializable;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.android.commands.monkey.ape.NodeVisitor;
import com.android.commands.monkey.ape.model.ActionType;
import com.android.commands.monkey.ape.naming.Name;
import com.android.commands.monkey.ape.naming.NameManager;
import com.android.commands.monkey.ape.naming.Namelet;
import com.android.commands.monkey.ape.utils.StringCache;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;

public class GUITreeNode implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2616531158905644587L;

    private String resourceId;
    private String className;
    private String packageName;
    private String text;
    private String contentDesc;

    private boolean enabled;
    private boolean checked;
    private boolean checkable;
    private boolean clickable;
    private boolean isFocusable;
    private boolean longClickable;
    private boolean scrollable;
    private boolean isPassword;

    private int descendantCount = 1; // inclusive, include itself
    private boolean focused;
    private int parentLeft;
    private int parentTop;
    private int parentRight;
    private int parentBottom;
    private int screenLeft;
    private int screenTop;
    private int screenRight;
    private int screenBottom;

    private Name xpathName;
    private transient Namelet currentNamelet;
    private transient Element domNode;

    private int index;
    private final GUITreeNode parent;
    private GUITreeNode children;
    private int childCount;
    private GUITreeNode sibling;
    private int depth = 1;
    private int height = 1;

    private String typeSignature;
    private String inputText;
    private int extraThrottle;

    private String indexPath;

    private Name tempXPathName;

    public GUITreeNode(GUITreeNode parent) {
        this.parent = parent;
        if (parent != null) {
            depth = parent.depth + 1;
        }
        this.text = "";
        this.className = "";
    }

    public String getIndexPath() {
        if (this.indexPath == null) {
            if (parent == null) {
                if (index == 0) {
                    return "0"; // avoid cache
                }
                return StringCache.cacheString(String.valueOf(getIndex()));
            } else {
                return StringCache.cacheString(parent.getIndexPath() + '-' + getIndex());
            }
        }
        return indexPath;
    }

    public int getDepth() {
        return this.depth;
    }

    void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return this.height;
    }

    public int getIndex() {
        return index;
    }

    public int getDescendantCount() {
        return this.descendantCount;
    }

    public boolean isWebView() {
        return getClassName().equals("android.webkit.WebView");
    }

    public void resetActions(ActionType[] actionTypes) {
        if (actionTypes == null) {
            return;
        }
        if (this.typeSignature != null) {
            throw new IllegalStateException("Actions cannot be reset after the type signature has been built.");
        }
        setCheckable(false);
        setClickable(false);
        setLongClickable(false);
        setScrollable(false);
        for (ActionType at : actionTypes) {
            switch (at) {
            case EVENT_START:
            case EVENT_RESTART:
            case EVENT_CLEAN_RESTART:
            case MODEL_BACK:
            case FUZZ:
            case EVENT_ACTIVATE:
                throw new IllegalStateException("Cannot set " + at + " to widget.");
            case EVENT_NOP:
                break;
            case MODEL_CLICK:
                setClickable(true);
                break;
            case MODEL_LONG_CLICK:
                setLongClickable(true);
                break;
            case MODEL_SCROLL_TOP_DOWN:
            case MODEL_SCROLL_BOTTOM_UP:
            case MODEL_SCROLL_LEFT_RIGHT:
            case MODEL_SCROLL_RIGHT_LEFT:
                setScrollable(true);
                break;
            default:
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public boolean isInWebView() {
        if (isWebView()) {
            return true;
        }
        if (parent == null) {
            return false;
        }
        return this.parent.isInWebView();
    }

    public static GUITreeNode buildEmptyNode(GUITreeNode parent) {
        GUITreeNode node = new GUITreeNode(parent);
        return node;
    }

    public boolean isEditText() {
        return this.className.equals("android.widget.EditText");
    }

    public GUITreeNode getRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getRoot();
    }

    public boolean isOutOfRoot() {
        GUITreeNode root = getRoot();
        return !root.getBoundsInScreen().intersect(screenLeft, screenTop, screenRight, screenBottom);
    }

    public boolean hasOutOfRootChild() {
        for (GUITreeNode child = children; child != null; child = child.sibling) {
            if (child.isOutOfRoot()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return screenLeft >= screenRight || screenTop >= screenBottom;
    }

    public String getTypeSignature() {
        return typeSignature;
    }

    public GUITreeNode getParent() {
        return parent;
    }

    public void addChild(GUITreeNode child) {
        childCount++;
        if (children == null) {
            children = child;
        } else {
            GUITreeNode p = children;
            while (p.sibling != null) {
                p = p.sibling;
            }
            p.sibling = child;
        }
        this.descendantCount += child.getDescendantCount();
        int newHeight = child.getHeight() + 1;
        if (newHeight > height) {
            this.height = newHeight;
        }

        if (domNode != null) {
            if (child.domNode != null) {
                domNode.appendChild(child.domNode);
            }
        }
    }

    public int getChildCount() {
        return childCount;
    }

    public Iterator<GUITreeNode> getChildren() {
        return new Iterator<GUITreeNode>() {
            GUITreeNode current = GUITreeNode.this.children;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public GUITreeNode next() {
                GUITreeNode ret = current;
                current = current.sibling;
                return ret;
            }
        };
    }

    public void visitNode(NodeVisitor visitor) {
        visitor.visit(this);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String pkg) {
        this.packageName = pkg;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String clazz) {
        if (this.className.equals(clazz)) {
            return;
        }
        this.className = clazz;
        if (this.domNode != null) {
            this.domNode.setAttribute("class", clazz);
        }
    }

    public String getContentDesc() {
        return contentDesc;
    }

    public void setContentDesc(String contentDesc) {
        this.contentDesc = contentDesc;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isCheckable() {
        return checkable;
    }

    public void setCheckable(boolean checkable) {
        this.checkable = checkable;
        if (domNode != null) {
            domNode.setAttribute("checkable", Boolean.toString(checkable));
        }
    }

    public boolean isPassword() {
        return this.isPassword;
    }

    public void setIsPassword(boolean value) {
        this.isPassword = value;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
        if (domNode != null) {
            domNode.setAttribute("clickable", String.valueOf(clickable));
        }
    }

    public boolean isLongClickable() {
        return longClickable;
    }

    public void setLongClickable(boolean longClickable) {
        this.longClickable = longClickable;
        if (domNode != null) {
            domNode.setAttribute("long-clickable", String.valueOf(longClickable));
        }
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
        if (domNode != null) {
            domNode.setAttribute("scrollable", String.valueOf(scrollable));
            domNode.setAttribute("scroll-type", String.valueOf(getScrollType()));
        }
    }

    public void setTempXPathName(Name name) {
        this.tempXPathName = name;
    }

    public Name getTempXPathName() {
        return this.tempXPathName;
    }

    public void setXPathName(Name name) {
        this.xpathName = NameManager.getCachedName(name);
    }

    public Name getXPathName() {
        return xpathName;
    }

    public Rect getBoundsInParent() {
        return new Rect(parentLeft, parentTop, parentRight, parentBottom);
    }

    public void setBoundsInParent(Rect boundsInParent) {
        this.parentLeft = boundsInParent.left;
        this.parentTop = boundsInParent.top;
        this.parentRight = boundsInParent.right;
        this.parentBottom = boundsInParent.bottom;
    }

    public Rect getBoundsInScreen() {
        return new Rect(screenLeft, screenTop, screenRight, screenBottom);
    }

    public void setBoundsInScreen(Rect boundsInScreen) {
        this.screenLeft = boundsInScreen.left;
        this.screenTop = boundsInScreen.top;
        this.screenRight = boundsInScreen.right;
        this.screenBottom = boundsInScreen.bottom;
    }

    public boolean compareType(GUITreeNode other) {
        return this.typeSignature.compareTo(other.typeSignature) == 0;
    }

    public int compareContents(GUITreeNode other) {
        if (!compareType(other)) {
            throw new IllegalArgumentException("Should be the same type");
        }
        return 0;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFocusable() {
        return isFocusable;
    }

    public void setFocusable(boolean isFocusable) {
        this.isFocusable = isFocusable;
    }

    public String getResourceID() {
        return resourceId;
    }

    public void setResourceID(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setInputText(String text) {
        this.inputText = text;
    }

    public String getInputText() {
        return this.inputText;
    }

    public void setIndex(int index) {
        if (this.index == index) {
            return;
        }
        this.index = index;
        if (domNode != null) {
            domNode.setAttribute("index", String.valueOf(index));
        }
    }

    public String getScrollType() {
        if (!isScrollable()) {
            return "none";
        }
        if (className.equals("android.widget.ScrollView") || className.equals("android.widget.ListView")
                || className.equals("android.widget.ExpandableListView")
                || className.equals("android.support.v17.leanback.widget.VerticalGridView")) {
            return "vertical";
        } else if (className.equals("android.widget.HorizontalScrollView")
                || className.equals("android.support.v17.leanback.widget.HorizontalGridView")
                || className.equals("android.support.v4.view.ViewPager")) {
            return "horizontal";
        }
        return "all";
    }

    public Element getDomNode() {
        if (domNode == null) {
            throw new IllegalStateException("Fetch document for the GUI tree first.");
        }
        return domNode;
    }

    public void setDomNode(Element domNode) {
        this.domNode = domNode;
        if (domNode != null) {
            domNode.setUserData(GUITreeBuilder.GUI_TREE_NODE_PROP_NAME, this, null);
        }
    }

    public Namelet getCurrentNamelet() {
        return currentNamelet;
    }

    public void setCurrentNamelet(Namelet currentNamelet) {
        this.currentNamelet = currentNamelet;
    }

    public int getExtraThrottle() {
        return extraThrottle;
    }

    public void setExtraThrottle(int extraThrottle) {
        this.extraThrottle = extraThrottle;
    }

    public void clearChildren() {
        this.childCount = 0;
        this.children = null;
        if (this.domNode != null) {
            NodeList childNodes = this.domNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                this.domNode.removeChild(childNodes.item(i));
            }
        }
    }

    public void removeChild(GUITreeNode node) {
        GUITreeNode p = children;
        if (p == null) {
            return;
        }
        if (p == node) {
            children = node.sibling;
            childCount --;
            if (this.domNode != null) {
                if (node.domNode != null) {
                    this.domNode.removeChild(node.domNode);
                }
            }
            return;
        }
        while (p.sibling != null) {
            if (p.sibling == node) {
                p.sibling = node.sibling;
                childCount --;
                if (this.domNode != null) {
                    if (node.domNode != null) {
                        this.domNode.removeChild(node.domNode);
                    }
                }
                return;
            }
            p = p.sibling;
        }
    }

    public void computeAndSetImageText(Bitmap image) {
        if (isEmpty()) {
            return;
        }
        if (!(isCheckable() || isClickable() || isScrollable() || isLongClickable())) {
            return;
        }
        if (!this.className.contains("ImageButton")) {
            return;
        }
        if (this.isOutOfRoot()) {
            return;
        }
        int width = this.screenRight - this.screenLeft;
        int height = this.screenBottom - this.screenTop;
        int x = this.screenLeft;
        int y = this.screenTop;
        int imageWidth = image.getWidth();
        int[] pixels = new int[height * imageWidth];
        if (x < 0 || y < 0) {
            return;
        }
        if (width < 2 || height < 2) {
            return;
        }
        if (x + width -1 > image.getWidth()) {
            return;
        }
        if (y + height -1 > image.getHeight()) {
            return;
        }
        image = image.copy(Config.RGB_565, true);
        if (image == null) {
            return;
        }
        image.getPixels(pixels, 0, imageWidth, x, y, width - 1, height - 1);
        int hash = 0;
        int begin = 0;
        for (int row = 0; row < height; row ++) {
            for (int i = begin; i < begin + width; i++) {
                hash = hash * 31 + pixels[i];
            }
            begin = begin + imageWidth;
        }
        image.recycle();
        setText(String.format("#%x", hash));
    }
}
