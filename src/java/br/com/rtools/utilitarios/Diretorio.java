package br.com.rtools.utilitarios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;

public class Diretorio {

    private final static String cliente = "";

    public static boolean criar(String diretorio) {
        return criar(diretorio, false);
    }

    public static boolean criar(String diretorio, boolean ignore) {
        if (diretorio.equals("cookie")) {
            diretorio = "/resources/global/cookie";
        } else if (!ignore) {
            diretorio = "/Cliente/" + getCliente() + "/" + diretorio;
        } else {
            diretorio = "/resources/cliente/" + getCliente().toLowerCase() + "/" + diretorio.toLowerCase();
        }
        try {
            String path = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath(diretorio);
            if (!new File(path).exists()) {
                File file = new File(path);
                if (!file.mkdirs()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean criarExterno(String diretorio, boolean externo) {
        diretorio = "D:/Cliente/" + getCliente() + "/" + diretorio;
        try {
            diretorio = diretorio.replaceAll("[\\\\]", "/");
            String s[] = diretorio.split("/");
            boolean err = false;
            String caminhoContac = "";
            int b = 0;
            String caminho = "";
            for (String item : s) {
                if (!item.equals("")) {
                    if (b == 0) {
                        caminhoContac = item;
                        caminho = "/" + caminhoContac;
                    } else {
                        caminhoContac = "/" + caminhoContac + "/" + item;
                        caminho = caminhoContac;
                    }
                    if (!new File(caminho).exists()) {
                        File file = new File(caminho);
                        if (!file.mkdir()) {
                            err = false;
                            break;
                        }
                    }
                    b++;
                }
            }
            if (!err) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean remover(String diretorio, boolean recursive) {
        try {
            FileUtils.deleteDirectory(new File(((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + getCliente() + "/" + diretorio)));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean remover(String diretorio) {
        if (new File(((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + getCliente() + "/" + diretorio)).exists()) {
            File file = new File(((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + getCliente() + "/" + diretorio));
            File[] arquivos = file.listFiles();
            for (File arquivo : arquivos) {
                arquivo.delete();
            }
            if (file.delete()) {
                return true;
            }
        }
        return false;
    }

    public static List listaArquivos(String diretorio) {
        List listaArquivos = new ArrayList();
        String caminho = "";
        try {
            caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + getCliente() + "/" + diretorio);
            File files = new File(caminho);
            if (!files.exists()) {
                caminho = "";
            }
        } catch (Exception ef) {
        }
        if (caminho == null || caminho.isEmpty()) {
            try {
                caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("") + "resources/cliente/" + getCliente().toLowerCase() + "/" + diretorio;
            } catch (Exception ef2) {
            }
        }
        try {
            File files = new File(caminho);
            if (!files.exists()) {
                return new ArrayList();
            }
            File listFile[] = files.listFiles();
            int numArq = listFile.length;
            int i = 0;
            while (i < numArq) {
                listaArquivos.add(new DataObject(listFile[i], listFile[i].getName(), i));
                i++;
            }
        } catch (Exception e) {
            return new ArrayList();
        }
        return listaArquivos;
    }

    public static List<MemoryFile> listMemoryFiles(String path) {
        List<MemoryFile> listaArquivos = new ArrayList<>();
        String caminho = "";
        try {
            caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + getCliente() + "/" + path);
            File files = new File(caminho);
            if (!files.exists()) {
                caminho = "";
            }
        } catch (Exception ef) {
        }
        if (caminho == null || caminho.isEmpty()) {
            try {
                caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("") + "resources/cliente/" + getCliente().toLowerCase() + "/" + path;
            } catch (Exception ef2) {
            }
        }
        try {
            File files = new File(caminho);
            if (!files.exists()) {
                return new ArrayList<>();
            }
            File listFile[] = files.listFiles();
            int numArq = listFile.length;
            int i = 0;
            while (i < numArq) {
                listaArquivos.add(new MemoryFile(listFile[i].getName(), listFile[i], i));
                i++;
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return listaArquivos;
    }

    public static String arquivo(String diretorio, String arquivo) {
        if (arquivo.isEmpty()) {
            return null;
        }
        String caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + getCliente() + "/" + diretorio + "/" + arquivo);
        try {
            File files = new File(caminho);
            if (!files.exists()) {
                return null;
            }
            return caminho;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCliente() {
        if (cliente.equals("")) {
            if (GenericaSessao.exists("sessaoCliente")) {
                return GenericaSessao.getString("sessaoCliente");
            }
        }
        return cliente;
    }

}
