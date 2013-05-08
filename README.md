# Vert.x Unzip Module

Simple worker module that given the file name of a zip file, unzips it to a temp directory or a specified directory and returns the filename in a Json message

## Configuration

Fields:

`address`: The address on the event bus where to listen for messages

## Usage

Send a Json message to `address`.

Fields:

* `zipFile`: Mandatory. Name of a zip file to unzip. It must exist
* `destDir`: Optional. Name of directory to unzip the file to. If not specified a temp directory will be generated. The destDir will be created if it does not exist already.
* `deleteZip`: Optional. Boolean. If `true` then the zip file will be deleted afterwards. Default is `false`.
