/*
 * Copyright (c) 1995, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util.zip;

import java.util.Date;

/** {@collect.stats} 
 * {@description.open}
 * This class is used to represent a ZIP file entry.
 * {@description.close}
 *
 * @author      David Connelly
 */
public
class ZipEntry implements ZipConstants, Cloneable {
    String name;        // entry name
    long time = -1;     // modification time (in DOS time)
    long crc = -1;      // crc-32 of entry data
    long size = -1;     // uncompressed size of entry data
    long csize = -1;    // compressed size of entry data
    int method = -1;    // compression method
    byte[] extra;       // optional extra field data for entry
    String comment;     // optional comment string for entry

    /** {@collect.stats} 
     * {@description.open}
     * Compression method for uncompressed entries.
     * {@description.close}
     */
    public static final int STORED = 0;

    /** {@collect.stats} 
     * {@description.open}
     * Compression method for compressed (deflated) entries.
     * {@description.close}
     */
    public static final int DEFLATED = 8;

    static {
        /* Zip library is loaded from System.initializeSystemClass */
        initIDs();
    }

    private static native void initIDs();

    /** {@collect.stats} 
     * {@description.open}
     * Creates a new zip entry with the specified name.
     * {@description.close}
     *
     * @param name the entry name
     * @exception NullPointerException if the entry name is null
     * @exception IllegalArgumentException if the entry name is longer than
     *            0xFFFF bytes
     */
    public ZipEntry(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name.length() > 0xFFFF) {
            throw new IllegalArgumentException("entry name too long");
        }
        this.name = name;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Creates a new zip entry with fields taken from the specified
     * zip entry.
     * {@description.close}
     * @param e a zip Entry object
     */
    public ZipEntry(ZipEntry e) {
        name = e.name;
        time = e.time;
        crc = e.crc;
        size = e.size;
        csize = e.csize;
        method = e.method;
        extra = e.extra;
        comment = e.comment;
    }

    /*
     * Creates a new zip entry for the given name with fields initialized
     * from the specified jzentry data.
     */
    ZipEntry(String name, long jzentry) {
        this.name = name;
        initFields(jzentry);
    }

    private native void initFields(long jzentry);

    /*
     * Creates a new zip entry with fields initialized from the specified
     * jzentry data.
     */
    ZipEntry(long jzentry) {
        initFields(jzentry);
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the name of the entry.
     * {@description.close}
     * @return the name of the entry
     */
    public String getName() {
        return name;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the modification time of the entry.
     * {@description.close}
     * @param time the entry modification time in number of milliseconds
     *             since the epoch
     * @see #getTime()
     */
    public void setTime(long time) {
        this.time = javaToDosTime(time);
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the modification time of the entry, or -1 if not specified.
     * {@description.close}
     * @return the modification time of the entry, or -1 if not specified
     * @see #setTime(long)
     */
    public long getTime() {
        return time != -1 ? dosToJavaTime(time) : -1;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the uncompressed size of the entry data.
     * {@description.close}
     * @param size the uncompressed size in bytes
     * @exception IllegalArgumentException if the specified size is less
     *            than 0 or greater than 0xFFFFFFFF bytes
     * @see #getSize()
     */
    public void setSize(long size) {
        if (size < 0 || size > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the uncompressed size of the entry data, or -1 if not known.
     * {@description.close}
     * @return the uncompressed size of the entry data, or -1 if not known
     * @see #setSize(long)
     */
    public long getSize() {
        return size;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the size of the compressed entry data, or -1 if not known.
     * In the case of a stored entry, the compressed size will be the same
     * as the uncompressed size of the entry.
     * {@description.close}
     * @return the size of the compressed entry data, or -1 if not known
     * @see #setCompressedSize(long)
     */
    public long getCompressedSize() {
        return csize;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the size of the compressed entry data.
     * {@description.close}
     * @param csize the compressed size to set to
     * @see #getCompressedSize()
     */
    public void setCompressedSize(long csize) {
        this.csize = csize;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the CRC-32 checksum of the uncompressed entry data.
     * {@description.close}
     * @param crc the CRC-32 value
     * @exception IllegalArgumentException if the specified CRC-32 value is
     *            less than 0 or greater than 0xFFFFFFFF
     * @see #getCrc()
     */
    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = crc;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known.
     * {@description.close}
     * @return the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known
     * @see #setCrc(long)
     */
    public long getCrc() {
        return crc;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the compression method for the entry.
     * {@description.close}
     * @param method the compression method, either STORED or DEFLATED
     * @exception IllegalArgumentException if the specified compression
     *            method is invalid
     * @see #getMethod()
     */
    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the compression method of the entry, or -1 if not specified.
     * {@description.close}
     * @return the compression method of the entry, or -1 if not specified
     * @see #setMethod(int)
     */
    public int getMethod() {
        return method;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the optional extra field data for the entry.
     * {@description.close}
     * @param extra the extra field data bytes
     * @exception IllegalArgumentException if the length of the specified
     *            extra field data is greater than 0xFFFF bytes
     * @see #getExtra()
     */
    public void setExtra(byte[] extra) {
        if (extra != null && extra.length > 0xFFFF) {
            throw new IllegalArgumentException("invalid extra field length");
        }
        this.extra = extra;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the extra field data for the entry, or null if none.
     * {@description.close}
     * @return the extra field data for the entry, or null if none
     * @see #setExtra(byte[])
     */
    public byte[] getExtra() {
        return extra;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Sets the optional comment string for the entry.
     * {@description.close}
     * @param comment the comment string
     * @exception IllegalArgumentException if the length of the specified
     *            comment string is greater than 0xFFFF bytes
     * @see #getComment()
     */
    public void setComment(String comment) {
        if (comment != null && comment.length() > 0xffff/3
                    && ZipOutputStream.getUTF8Length(comment) > 0xffff) {
            throw new IllegalArgumentException("invalid entry comment length");
        }
        this.comment = comment;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the comment string for the entry, or null if none.
     * {@description.close}
     * @return the comment string for the entry, or null if none
     * @see #setComment(String)
     */
    public String getComment() {
        return comment;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns true if this is a directory entry. A directory entry is
     * defined to be one whose name ends with a '/'.
     * {@description.close}
     * @return true if this is a directory entry
     */
    public boolean isDirectory() {
        return name.endsWith("/");
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns a string representation of the ZIP entry.
     * {@description.close}
     */
    public String toString() {
        return getName();
    }

    /*
     * Converts DOS time to Java time (number of milliseconds since epoch).
     */
    private static long dosToJavaTime(long dtime) {
        Date d = new Date((int)(((dtime >> 25) & 0x7f) + 80),
                          (int)(((dtime >> 21) & 0x0f) - 1),
                          (int)((dtime >> 16) & 0x1f),
                          (int)((dtime >> 11) & 0x1f),
                          (int)((dtime >> 5) & 0x3f),
                          (int)((dtime << 1) & 0x3e));
        return d.getTime();
    }

    /*
     * Converts Java time to DOS time.
     */
    private static long javaToDosTime(long time) {
        Date d = new Date(time);
        int year = d.getYear() + 1900;
        if (year < 1980) {
            return (1 << 21) | (1 << 16);
        }
        return (year - 1980) << 25 | (d.getMonth() + 1) << 21 |
               d.getDate() << 16 | d.getHours() << 11 | d.getMinutes() << 5 |
               d.getSeconds() >> 1;
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns the hash code value for this entry.
     * {@description.close}
     */
    public int hashCode() {
        return name.hashCode();
    }

    /** {@collect.stats} 
     * {@description.open}
     * Returns a copy of this entry.
     * {@description.close}
     */
    public Object clone() {
        try {
            ZipEntry e = (ZipEntry)super.clone();
            e.extra = (extra == null) ? null : extra.clone();
            return e;
        } catch (CloneNotSupportedException e) {
            // This should never happen, since we are Cloneable
            throw new InternalError();
        }
    }
}
