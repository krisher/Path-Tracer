package edu.rit.krisher.fileparser.ply;

/**
 * Format of the PLY file content, ascii or binary.
 */
public enum PLYFormat {
   /**
    * Format for ascii representation of element data.
    */
   ascii,
   /**
    * Format for binary representation of element data.
    */
   binary_little_endian,
   /**
    * Format for binary representation of element data.
    */
   binary_big_endian
}