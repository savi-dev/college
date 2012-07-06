// Copyright (C) 2012, The SAVI Project.
package ca.savi.college.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ca.savi.horse.model.FPGA;
import ca.savi.horse.model.hwparams.GenericOpRequestParams;
import ca.savi.horse.model.hwparams.GetRequestParams;
import ca.savi.horse.model.hwparams.InitRequestParams;
import ca.savi.horse.model.hwparams.ListRequestParams;
import ca.savi.horse.model.hwparams.ReleaseRequestParams;
import ca.savi.horse.model.hwparams.StatusResponseParams;
import ca.savi.king.client.SaviCLI;
import ca.savi.king.ws.ResourceConnectToNetworkResponse;
import ca.savi.king.ws.ResourceDisconnectFromNetworkResponse;
import ca.savi.king.ws.ResourceProgramResponse;

/**
 * This is Hardware Resource Client
 * @author Hesam Rahimi Koopayi <hesam.rahimikoopayi@utoronto.ca>
 * @version 0.1
 */
public class CHwCLI extends SaviCLI {
  public final String resType = "Hardware";

  /**
   * Constructor.
   * @param args
   */
  public CHwCLI(String[] args) {
    this.processCommand(args);
  }

  public CommandLine buildCLIOptions(String args[]) {
    opt = new Options();
    Option tempOpt = new Option("c", true, "command");
    tempOpt.setRequired(true);
    opt.addOption(tempOpt);
    tempOpt = new Option("u", true, "username");
    tempOpt.setRequired(true);
    opt.addOption(tempOpt);
    tempOpt = new Option("p", true, "password");
    tempOpt.setRequired(true);
    opt.addOption(tempOpt);
    tempOpt = new Option("wsdl", true, "control wsdl_location");
    tempOpt.setRequired(false);
    opt.addOption(tempOpt);
    tempOpt = new Option("ns", true, "control namespace");
    tempOpt.setRequired(false);
    opt.addOption(tempOpt);
    tempOpt = new Option("sn", true, "control service");
    tempOpt.setRequired(false);
    opt.addOption(tempOpt);
    opt.addOption("j", true, "project_name");
    opt.addOption("l", true, "node_name");
    opt.addOption("r", true, "res_type");
    opt.addOption("h", true, "help");
    opt.addOption("uu", true, "uuid");
    opt.addOption("x", true, "xml");
    opt.addOption("puu", true, "port_uuid");
    opt.addOption("nuu", true, "net_uuid");
    opt.addOption("npuu", true, "netport_uuid");
    opt.addOption("iuu", true, "image_uuid");
    // opt.addOption("a", true, "bitfile_path"); // the path of the bit file
    // subject to be programmed on the
    // FPGA. This file should be on
    // the storage resource
    opt.addOption("v", true, "reg_val"); // register value to be written on the
                                         // FPGA register
    opt.addOption("uu", true, "uuid"); // uuid
    CommandLine cmd = null;
    CommandLineParser parser = new PosixParser();
    try {
      cmd = parser.parse(opt, args);
    } catch (ParseException ex) {
      System.out.println("Error:" + ex.getMessage());
    }
    return cmd;
  }

  public void processCommand(String args[]) {
    CommandLine cmd = buildCLIOptions(args);
    if (cmd == null) {
      System.out.print("not enough arguments!");
      printHelp();
      System.exit(1);
      return;
    }
    String wsdl = cmd.getOptionValue("wsdl");
    String namespace = cmd.getOptionValue("ns");
    String service = cmd.getOptionValue("sn");
    String command = cmd.getOptionValue("c");
    String user = cmd.getOptionValue("u");
    String password = cmd.getOptionValue("p");
    String nodeName = cmd.getOptionValue("l");
    String tenantName = cmd.getOptionValue("j");
    String uuid = cmd.getOptionValue("uu");
    // from my own
    // String bitFilePath = cmd.getOptionValue("a");
    String reg_value = cmd.getOptionValue("v");
    String portUuid = cmd.getOptionValue("puu");
    String networkUuid = cmd.getOptionValue("nuu");
    String networkPortUuid = cmd.getOptionValue("npuu");
    String imageUuid = cmd.getOptionValue("iuu");
    // end
    if (command == null) {
      printHelp();
      System.exit(1);
      return;
    }
    if (wsdl != null) {
      System.out.println(wsdl);
      setControlServerWSDLlocation(wsdl);
    }
    if (namespace != null) {
      setControlSericeNamespace(namespace);
    }
    if (service != null) {
      setControlServiceName(service);
    }
    // from my own
    if (command.equals("init_res")) {
      hwCLIinitializeHardware(user, password, nodeName);
    } else if (command.equals("read_res")) {
      hwCLIreadFromRegister(user, password, nodeName, uuid);
    } else if (command.equals("write_res")) {
      hwCLIwriteToRegister(user, password, nodeName, uuid, reg_value);
    } else if (command.equals("get_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      ret &= checkRequiredOption(tenantName, "proj_name");
      if (!ret)
        return;
      hwCLIgetHardwareResource(user, password, nodeName, tenantName, uuid);
    } else if (command.equals("list_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      if (!ret)
        return;
      hwCLIlistAvailableResources(user, password, nodeName);
    } else if (command.equals("prg_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      ret &= checkRequiredOption(tenantName, "proj_name");
      ret &= checkRequiredOption(uuid, "uuid");
      ret &= checkRequiredOption(uuid, "image_uuid");
      if (!ret)
        return;
      hwCLIprogramHardwareResource(user, password, nodeName, tenantName, uuid,
          imageUuid, null);
    } else if (command.equals("rel_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      ret &= checkRequiredOption(tenantName, "proj_name");
      ret &= checkRequiredOption(uuid, "uuid");
      if (!ret)
        return;
      hwCLIrelProcessingNode(user, password, nodeName, tenantName, uuid);
    } else if (command.equals("stat_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      ret &= checkRequiredOption(tenantName, "proj_name");
      ret &= checkRequiredOption(uuid, "uuid");
      if (!ret)
        return;
      hwCLIstatusResource(user, password, nodeName, tenantName, uuid);
    } else if (command.equals("connect_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      ret &= checkRequiredOption(tenantName, "proj_name");
      ret &= checkRequiredOption(uuid, "uuid");
      ret &= checkRequiredOption(portUuid, "port_uuid");
      ret &= checkRequiredOption(networkUuid, "net_uuid");
      if (!ret)
        return;
      hwCLIconnectResourceToNetwork(user, password, "", nodeName, tenantName,
          resType, uuid, portUuid, networkUuid, null);
    } else if (command.equals("disconnect_res")) {
      boolean ret = true;
      ret &= checkRequiredOption(nodeName, "node_name");
      ret &= checkRequiredOption(tenantName, "proj_name");
      ret &= checkRequiredOption(uuid, "uuid");
      ret &= checkRequiredOption(portUuid, "port_uuid");
      ret &= checkRequiredOption(networkUuid, "net_uuid");
      ret &= checkRequiredOption(networkPortUuid, "netport_uuid");
      if (!ret)
        return;
      hwCLIdisconnectResourceFromNetwork(user, password, "", nodeName,
          tenantName, resType, uuid, portUuid, networkUuid, networkPortUuid,
          null);
    } else if (command.equals("help")) {
      printHelp();
    } else {
      printHelp();
      // processCommand(args);
    }
  }

  public void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("hwcli", opt);
  }

  private void hwCLIreadFromRegister(String user, String password,
      String nodeName, String uuid) {
    String xmlString = "";
    if (uuid != null) {
      GenericOpRequestParams params = new GenericOpRequestParams();
      params.setUuid(uuid);
      StringWriter stringWriter = new StringWriter();
      JAXBContext context;
      try {
        context = JAXBContext.newInstance(GenericOpRequestParams.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(
            new JAXBElement(new QName("", GenericOpRequestParams.class
                .getName()), GenericOpRequestParams.class, params),
            stringWriter);
        System.out.println("XMl is : " + stringWriter.toString());
        xmlString = stringWriter.toString();
      } catch (Exception ex) {
        System.out.println("exception in getProceNode: " + ex.getMessage());
      }
    }
    genericOperationResources(user, password, nodeName, resType, xmlString);
  }

  private void hwCLIwriteToRegister(String user, String password,
      String nodeName, String uuid, String reg_value) {
    String xmlString = "";
    if (uuid != null) {
      GenericOpRequestParams params = new GenericOpRequestParams();
      params.setUuid(uuid);
      if (reg_value != null) {
        params.setSetRegisterValue(fromHexString(reg_value));
      }
      StringWriter stringWriter = new StringWriter();
      JAXBContext context;
      try {
        context = JAXBContext.newInstance(GenericOpRequestParams.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(
            new JAXBElement(new QName("", GenericOpRequestParams.class
                .getName()), GenericOpRequestParams.class, params),
            stringWriter);
        System.out.println("XMl is : " + stringWriter.toString());
        xmlString = stringWriter.toString();
      } catch (Exception ex) {
        System.out.println("exception in getProceNode: " + ex.getMessage());
      }
    }
    genericOperationResources(user, password, nodeName, resType, xmlString);
  }

  // from http://mindprod.com/jgloss/hex.html
  private byte[] fromHexString(String s) {
    int stringLength = s.length();
    if ((stringLength & 0x1) != 0) {
      throw new IllegalArgumentException(
          "fromHexString requires an even number of hex characters");
    }
    byte[] b = new byte[stringLength / 2];
    for (int i = 0, j = 0; i < stringLength; i += 2, j++) {
      int high = charToNibble(s.charAt(i));
      int low = charToNibble(s.charAt(i + 1));
      b[j] = (byte) ((high << 4) | low);
    }
    return b;
  }

  // from http://mindprod.com/jgloss/hex.html
  private int charToNibble(char c) {
    if ('0' <= c && c <= '9') {
      return c - '0';
    } else if ('a' <= c && c <= 'f') {
      return c - 'a' + 0xa;
    } else if ('A' <= c && c <= 'F') {
      return c - 'A' + 0xa;
    } else {
      throw new IllegalArgumentException("Invalid hex character: " + c);
    }
  }

  private void hwCLIinitializeHardware(String user, String password,
      String nodeName) {
    String xmlString = "";
    InitRequestParams params = new InitRequestParams();
    StringWriter stringWriter = new StringWriter();
    JAXBContext context;
    try {
      context = JAXBContext.newInstance(InitRequestParams.class);
      Marshaller m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      m.marshal(new JAXBElement(
          new QName("", InitRequestParams.class.getName()),
          InitRequestParams.class, params), stringWriter);
      System.out.println("XMl is : " + stringWriter.toString());
      xmlString = stringWriter.toString();
    } catch (Exception ex) {
      System.out.println("exception in getProceNode: " + ex.getMessage());
    }
    initResources(user, password, nodeName, resType, xmlString);
  }

  private void hwCLIstatusResource(String user, String password,
      String nodeName, String tenantName, String uuid) {
    String xmlString = "";
    StatusResponseParams params = new StatusResponseParams();
    StringWriter stringWriter = new StringWriter();
    JAXBContext context;
    try {
      context = JAXBContext.newInstance(StatusResponseParams.class);
      Marshaller m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      m.marshal(
          new JAXBElement(new QName("", StatusResponseParams.class.getName()),
              StatusResponseParams.class, params), stringWriter);
      System.out.println("XMl is : " + stringWriter.toString());
      xmlString = stringWriter.toString();
    } catch (Exception ex) {
      System.out.println("exception in getProceNode: " + ex.getMessage());
    }
    statusResource(user, password, nodeName, tenantName, resType, uuid,
        xmlString);
  }

  public void hwCLIlistAvailableResources(String user, String password,
      String nodeName) {
    String xmlString = "";
    ListRequestParams params = new ListRequestParams();
    params.setVerbose(Boolean.TRUE);
    StringWriter stringWriter = new StringWriter();
    JAXBContext context;
    try {
      context = JAXBContext.newInstance(ListRequestParams.class);
      Marshaller m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      m.marshal(new JAXBElement(
          new QName("", ListRequestParams.class.getName()),
          ListRequestParams.class, params), stringWriter);
      System.out.println("XMl is : " + stringWriter.toString());
      xmlString = stringWriter.toString();
    } catch (Exception ex) {
      System.out.println("exception in getProceNode: " + ex.getMessage());
    }
    listResources(user, password, nodeName, resType, xmlString);
  }

  public void hwCLIgetHardwareResource(String user, String password,
      String nodeName, String tenantName, String uuid) {
    String xmlString = "";
    if (uuid != null) {
      GetRequestParams params = new GetRequestParams();
      params.setUuid(uuid);
      FPGA fpga = new FPGA("fpga1");
      fpga.getLink().add("link1");
      fpga.setUuid(uuid);
      params.getFPGA().add(fpga);
      params.setRawEthernet(Boolean.TRUE);
      params.setLRWidth(16);
      params.setRLWidth(16);
      StringWriter stringWriter = new StringWriter();
      JAXBContext context;
      try {
        context = JAXBContext.newInstance(GetRequestParams.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(
            new JAXBElement(new QName("", GetRequestParams.class.getName()),
                GetRequestParams.class, params), stringWriter);
        System.out.println("XMl is : " + stringWriter.toString());
        xmlString = stringWriter.toString();
      } catch (Exception ex) {
        System.out.println("exception in getProceNode: " + ex.getMessage());
      }
    }
    getResource(user, password, nodeName, tenantName, resType, xmlString);
  }

  private void hwCLIrelProcessingNode(String user, String password,
      String nodeName, String tenantName, String uuid) {
    String xmlString = "";
    if (uuid != null) {
      ReleaseRequestParams params = new ReleaseRequestParams();
      StringWriter stringWriter = new StringWriter();
      JAXBContext context;
      try {
        context = JAXBContext.newInstance(ReleaseRequestParams.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(
            new JAXBElement(
                new QName("", ReleaseRequestParams.class.getName()),
                ReleaseRequestParams.class, params), stringWriter);
        System.out.println("XMl is : " + stringWriter.toString());
        xmlString = stringWriter.toString();
      } catch (Exception ex) {
        System.out.println("exception in getProceNode: " + ex.getMessage());
      }
    }
    relResource(user, password, nodeName, tenantName, resType, uuid, xmlString);
  }

  public void hwCLIconnectResourceToNetwork(String user, String password,
      String token, String nodeUuid, String tenantUuid, String resType,
      String uuid, String portUuid, String networkUuid, String xmlString) {
    ResourceConnectToNetworkResponse res;
    res =
        connectResourceToNetwork(user, password, token, nodeUuid, tenantUuid,
            resType, uuid, portUuid, networkUuid, xmlString);
    if (res.getResult().isSuccessful()) {
      System.out.println("Your resource has been attached to this port: "
          + res.getNetworkPortUuid());
      System.out.println("in this network: " + networkUuid);
    } else {
      System.out.println("Error: " + res.getResult().getErrorStr());
    }
  }

  public void hwCLIdisconnectResourceFromNetwork(String user, String password,
      String token, String nodeUuid, String tenantUuid, String resType,
      String uuid, String portUuid, String networkUuid, String networkPortUuid,
      String xmlString) {
    ResourceDisconnectFromNetworkResponse res;
    res =
        disconnectResourceFromNetwork(user, password, token, nodeUuid,
            tenantUuid, resType, uuid, portUuid, networkUuid, networkPortUuid,
            xmlString);
    if (res.getResult().isSuccessful()) {
      System.out.println("Your resource has been disconnected.");
    } else {
      System.out.println("Error: " + res.getResult().getErrorStr());
    }
  }

  public void hwCLIprogramHardwareResource(String user, String password,
      String nodeName, String tenantName, String uuid, String imageUuid,
      String xmlString) {
    ResourceProgramResponse res;
    res =
        resourceProgram(user, password, nodeName, tenantName, resType, uuid,
            imageUuid, xmlString);
    // res = resourceProgram(user, password, nodeName, tenantName, resType,
    // uuid, xmlString);
    if (res.getResult().isSuccessful()) {
      System.out.println("Program Successful!");
    } else {
      System.out.print("Error: " + res.getResult().getErrorStr());
    }
  }

  public byte[] getBytesFromFile(File file) throws IOException {
    InputStream is = new FileInputStream(file);
    // Get the size of the file
    long length = file.length();
    // You cannot create an array using a long type.
    // It needs to be an int type.
    // Before converting to an int type, check
    // to ensure that file is not larger than Integer.MAX_VALUE.
    if (length > Integer.MAX_VALUE) {
      // File is too large
    }
    // Create the byte array to hold the data
    byte[] bytes = new byte[(int) length];
    // Read in the bytes
    int offset = 0;
    int numRead = 0;
    while (offset < bytes.length
        && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }
    // Ensure all the bytes have been read in
    if (offset < bytes.length) {
      throw new IOException("Could not completely read file " + file.getName());
    }
    // Close the input stream and return bytes
    is.close();
    return bytes;
  }
}
