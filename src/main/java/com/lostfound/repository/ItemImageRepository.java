
package com.lostfound.repository;

import com.lostfound.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemImageRepository
        extends JpaRepository<ItemImage, Long> {

    List<ItemImage> findByLostItemId(Long lostItemId);

    List<ItemImage> findByFoundItemId(Long foundItemId);

    void deleteByLostItemId(Long lostItemId);

    void deleteByFoundItemId(Long foundItemId);
}
