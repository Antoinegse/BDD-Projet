package com.project.artconnect.util;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.persistence.JdbcArtistDao;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.persistence.JdbcCommunityMemberDao;
import com.project.artconnect.persistence.JdbcExhibitionDao;
import com.project.artconnect.persistence.JdbcGalleryDao;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

/**
 * Fournisseur de services singleton.
 * FIX #1 : JdbcExhibitionDao est maintenant instancié et injecté dans ExhibitionService.
 */
public class ServiceProvider {

    private static final ArtistService     artistService;
    private static final ArtworkService    artworkService;
    private static final GalleryService    galleryService;
    private static final WorkshopService   workshopService;
    private static final CommunityService  communityService;
    private static final ExhibitionService exhibitionService; // FIX #1

    static {
        ArtistDao          artistDao          = new JdbcArtistDao();
        ArtworkDao         artworkDao         = new JdbcArtworkDao();
        GalleryDao         galleryDao         = new JdbcGalleryDao();
        WorkshopDao        workshopDao        = new JdbcWorkshopDao();
        CommunityMemberDao communityMemberDao = new JdbcCommunityMemberDao();
        ExhibitionDao      exhibitionDao      = new JdbcExhibitionDao(); // FIX #1

        artistService     = new JdbcArtistService(artistDao);
        artworkService    = new JdbcArtworkService(artworkDao);
        galleryService    = new JdbcGalleryService(galleryDao);
        workshopService   = new JdbcWorkshopService(workshopDao);
        communityService  = new JdbcCommunityService(communityMemberDao);
        exhibitionService = new JdbcExhibitionService(exhibitionDao); // FIX #1
    }

    public static ArtistService     getArtistService()     { return artistService;     }
    public static ArtworkService    getArtworkService()    { return artworkService;    }
    public static GalleryService    getGalleryService()    { return galleryService;    }
    public static WorkshopService   getWorkshopService()   { return workshopService;   }
    public static CommunityService  getCommunityService()  { return communityService;  }
    public static ExhibitionService getExhibitionService() { return exhibitionService; } // FIX #1
}
