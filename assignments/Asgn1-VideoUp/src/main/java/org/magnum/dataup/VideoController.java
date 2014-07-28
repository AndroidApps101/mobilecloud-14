package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

    private VideoFileManager manager = VideoFileManager.get();
    private List<Video> videos = new ArrayList<Video>();

    public VideoController() throws IOException {
    }

    @RequestMapping(method = RequestMethod.GET, value = "/video/{id}")
    public @ResponseBody Video getVideo(@PathVariable("id") int videoId) {
        return videos.get(videoId - 1);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/video")
    public @ResponseBody Collection<Video> getAllVideos() {
        return videos;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/video")
    public @ResponseBody Video postVideo(@RequestBody Video video) {
        long videoId = addAndGetId(video);
        video.setId(videoId);
        video.setDataUrl(getDataUrl(videoId));
        return video;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/video/{id}/data")
    public void getVideoData(@PathVariable("id") long videoId,
            HttpServletResponse response) throws IOException {
        if (videoId <= 0 || videoId > videos.size()) {
            throw new ResourceNotFoundException("Invalid id: " + videoId);
        }

        Video video = videos.get((int)videoId - 1);
        OutputStream out = response.getOutputStream();
        manager.copyVideoData(video, out);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/video/{id}/data")
    public @ResponseBody VideoStatus postVideoData(@PathVariable("id") long videoId,
            @RequestParam("data") MultipartFile videoData) throws IOException {
        if (videoId <= 0 || videoId > videos.size()) {
            throw new ResourceNotFoundException("Invalid id: " + videoId);
        }

        Video video = videos.get((int)videoId - 1);
        InputStream in = videoData.getInputStream();
        manager.saveVideoData(video, in);
        
        return new VideoStatus(VideoStatus.VideoState.READY);
    }

    private synchronized int addAndGetId(Video v) {
        videos.add(v);
        return videos.size();
    }

    private String getDataUrl(long videoId) {
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();
        String base = "http://"
                + request.getServerName()
                + ((request.getServerPort() != 80) ? ":"
                        + request.getServerPort() : "");
        return base;
    }
}
