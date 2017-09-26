package org.marvin.util

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

import scala.collection.mutable.ListBuffer

/**
  * Created by taka on 12/09/17.
  */
object HdfsUtil {
  def copyFromLocal(hdfsConf: Configuration, srcUri: String, dstUri: String) = {
    val fs = FileSystem.get(hdfsConf)
    fs.copyFromLocalFile(new Path(srcUri), new Path(dstUri))
    fs.close()
  }

  def copyToLocal(hdfsConf: Configuration, srcUri: String, dstUri: String) ={
    val fs = FileSystem.get(hdfsConf)
    fs.copyToLocalFile(false, new Path(srcUri), new Path(dstUri), false)
    fs.close()
  }

  def generateUris(artifacts:List[String], localRootPath:String, remoteRootPath:String, engineName:String, engineVersion:String):List[Map[String, String]]={
    val urls = ListBuffer[Map[String, String]]()
    for(artifactName <- artifacts){
      urls +=  Map(
        "localUri" -> s"$localRootPath/$engineName/$artifactName",
        "remoteUri" -> s"$remoteRootPath/$engineName/$engineVersion/$artifactName"
      )
    }
    return urls.toList
  }
}
