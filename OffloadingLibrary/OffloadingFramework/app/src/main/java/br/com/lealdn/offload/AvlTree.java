/*
 * Offloading Library -  ULOOF Project
 *
 * Copyright (C) 2017-2018  Stefano Secci <stefano.secci@cnam.fr>
 * Copyright (C) 2017-2018  Alessio Diamanti <alessio.diama@gmail.com>
 * Copyright (C) 2017-2018  José Leal Neto - Federal University of Minas Gerais
 * Copyright (C) 2017-2018  Daniel F. Macedo - Federal University of Minas Gerais
 * Copyright (C) 2017-2018  Alessio Mora	<mora.alessio20@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package br.com.lealdn.offload;

public class AvlTree<T extends Comparable<T>> {

    /**
    * Construct the tree.
    */
    public AvlTree() {
        root = null;
    }

    /**
    * Insert into the tree; duplicates are ignored.
    * @param x the item to insert.
    */
    public void insert(T x) {
        root = insertNode(x, root );
    }

    /**
    * Remove from the tree. Nothing is done if x is not found.
    * @param x the item to remove.
    */
    public void remove(T x ) {
        System.out.println( "Sorry, remove unimplemented" );
    }

    /**
    * Find the smallest item in the tree.
    * @return smallest item or null if empty.
    */
    public T findMin( ) {
        return elementAt( findMinNode( root ) );
    }

    /**
    * Find the largest item in the tree.
    * @return the largest item of null if empty.
    */
    public T findMax( ) {
        return elementAt( findMaxNode( root ) );
    }

    /**
    * Find an item in the tree.
    * @param x the item to search for.
    * @return the matching item or null if not found.
    */
    public T find(T x ) {
        return elementAt( findNode( x, root ) );
    }

    public boolean markNodeAsDirty(T x) {
        AvlNode<T> node = findNode(x, root);
        if (node != null) {
            node.setDirty(true);
            return true;
        }
        return false;
    }

    public AvlNode<T>[] findBetween(T x) {
        return findBetweenNode(x, root);
    }

    /**
    * Make the tree logically empty.
    */
    public void makeEmpty( ) {
        root = null;
    }

    /**
    * Test if the tree is logically empty.
    * @return true if empty, false otherwise.
    */
    public boolean isEmpty( ) {
        return root == null;
    }

    /**
    * Print the tree contents in sorted order.
    */
    public void printTree( ) {
        if( isEmpty( ) ) {
            System.out.println( "Empty tree" );
        } else { 
            printTreeNode( root );
        }
    }

    /**
    * Internal method to get element field.
    * @param t the node.
    * @return the element field or null if t is null.
    */
    private T elementAt(AvlNode<T> t)
    {
        return t == null ? null : t.getElement();
    }

    /**
    * Internal method to insert into a subtree.
    * @param x the item to insert.
    * @param t the node that roots the tree.
    * @return the new root.
    */
    private AvlNode<T> insertNode(T x, AvlNode<T> t) {
        if( t == null ) {
            t = new AvlNode<T>( x, null, null );
        } else if( x.compareTo( t.getElement() ) < 0 ) {
            t.setLeft(insertNode( x, t.getLeft() ));
            if( height( t.getLeft() ) - height( t.getRight() ) == 2 ) {
                if( x.compareTo( t.getLeft().getElement() ) < 0 ) {
                    t = rotateWithLeftChild( t );
                } else {
                    t = doubleWithLeftChild( t );
                }
            }
        }
        else if( x.compareTo( t.getElement() ) > 0 ) {
            t.setRight(insertNode( x, t.getRight() ));
            if( height( t.getRight() ) - height( t.getLeft() ) == 2 ) {
                if( x.compareTo( t.getRight().getElement() ) > 0 ) {
                    t = rotateWithRightChild( t );
                } else {
                    t = doubleWithRightChild( t );
                }
            }
        }

        t.setHeight(max( height( t.getLeft() ), height( t.getRight() ) ) + 1);
        return t;
    }

    /**
    * Internal method to find the smallest item in a subtree.
    * @param t the node that roots the tree.
    * @return node containing the smallest item.
    */
    private AvlNode<T> findMinNode(AvlNode<T> t) {
        if( t == null )
            return t;

        while( t.getLeft() != null ) {
            t = t.getLeft();
        }
        return t;
    }

    /**
    * Internal method to find the largest item in a subtree.
    * @param t the node that roots the tree.
    * @return node containing the largest item.
    */
    private AvlNode<T> findMaxNode(AvlNode<T> t) {
        if( t == null )
            return t;

        while( t.getRight() != null ) {
            t = t.getRight();
        }
        return t;
    }

    /**
    * Internal method to find an item in a subtree.
    * @param x is item to search for.
    * @param t the node that roots the tree.
    * @return node containing the matched item.
    */
    private AvlNode<T> findNode(T x, AvlNode<T> t) {
        while( t != null ) {
            if( x.compareTo( t.getElement() ) < 0 ) {
                t = t.getLeft();
            } else if( x.compareTo( t.getElement() ) > 0 ) {
                t = t.getRight();
            } else {
                return t;    // Match
            }
        }

        return null;   // No match
    }

    private AvlNode<T>[] findBetweenNode(T x, AvlNode<T> t) {
        AvlNode<T> high = null;
        AvlNode<T> low = null;

        while(x != null && t != null) {
            if (x.compareTo(t.getElement()) == 0) {
                return new AvlNode[]{ t };
            }

            if (x.compareTo(t.getElement()) < 0) {
                high = t;
                t = t.getLeft();
            }
            else {
                low = t;
                t = t.getRight();
            }
        }

        return new AvlNode[] { low, high };
    }

    public void markAllAsClean() {
        markAllAsCleanNodes(root);
    }

    private void markAllAsCleanNodes(AvlNode<T> node) {
        if (node != null) {
            node.setDirty(false);
            markAllAsCleanNodes(node.getLeft());
            markAllAsCleanNodes(node.getRight());
        }
    }

    /**
    * Internal method to print a subtree in sorted order.
    * @param t the node that roots the tree.
    */
    private void printTreeNode(AvlNode<T> t) {
        if( t != null ) {
            printTreeNode( t.getLeft() );
            System.out.println( t.getElement() );
            printTreeNode( t.getRight() );
        }
    }

    /**
    * Return the height of node t, or -1, if null.
    */
    private int height(AvlNode<T> t) {
        return t == null ? -1 : t.getHeight();
    }

    /**
    * Return maximum of lhs and rhs.
    */
    private int max( int lhs, int rhs ) {
        return lhs > rhs ? lhs : rhs;
    }

    /**
    * Rotate binary tree node with left child.
    * For AVL trees, this is a single rotation for case 1.
    * Update heights, then return new root.
    */
    private AvlNode<T> rotateWithLeftChild(AvlNode<T> k2) {
        AvlNode<T> k1 = k2.getLeft();
        k2.setLeft(k1.getRight());
        k1.setRight(k2);
        k2.setHeight(max( height( k2.getLeft() ), height( k2.getRight() ) ) + 1);
        k1.setHeight(max( height( k1.getLeft() ), k2.getHeight() ) + 1);
        return k1;
    }

    /**
    * Rotate binary tree node with right child.
    * For AVL trees, this is a single rotation for case 4.
    * Update heights, then return new root.
    */
    private AvlNode<T> rotateWithRightChild(AvlNode<T> k1) {
        AvlNode<T> k2 = k1.getRight();
        k1.setRight(k2.getLeft());
        k2.setLeft(k1);
        k1.setHeight(max( height( k1.getLeft() ), height( k1.getRight() ) ) + 1);
        k2.setHeight(max( height( k2.getRight() ), k1.getHeight() ) + 1);
        return k2;
    }

    /**
    * Double rotate binary tree node: first left child
    * with its right child; then node k3 with new left child.
    * For AVL trees, this is a double rotation for case 2.
    * Update heights, then return new root.
    */
    private AvlNode<T> doubleWithLeftChild(AvlNode<T> k3) {
        k3.setLeft(rotateWithRightChild( k3.getLeft() ));
        return rotateWithLeftChild( k3 );
    }

    /**
    * Double rotate binary tree node: first right child
    * with its left child; then node k1 with new right child.
    * For AVL trees, this is a double rotation for case 3.
    * Update heights, then return new root.
    */
    private AvlNode<T> doubleWithRightChild(AvlNode<T> k1) {
        k1.setRight(rotateWithLeftChild( k1.getRight() ));
        return rotateWithRightChild( k1 );
    }

    /** The tree root. */
    private AvlNode<T> root;
}
