package com.example.photoviewer;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter
        extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    public interface OnImageClickListener {
        void onImageClick(PostItem item);
    }

    private final List<PostItem> postList;
    private final boolean gridMode;
    private final OnImageClickListener clickListener;

    public ImageAdapter(
            List<PostItem> postList,
            boolean gridMode,
            OnImageClickListener clickListener
    ) {
        this.postList = postList;
        this.gridMode = gridMode;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);

        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ImageViewHolder holder,
            int position
    ) {
        PostItem item = postList.get(position);
        holder.imageView.setImageBitmap(item.getBitmap());
        holder.titleView.setText(item.getTitle());

        int heightDp = gridMode ? 140 : 200;
        float density = holder.itemView.getResources()
                .getDisplayMetrics().density;
        ViewGroup.LayoutParams params =
                holder.imageView.getLayoutParams();
        params.height = Math.round(heightDp * density);
        holder.imageView.setLayoutParams(params);

        holder.itemView.setOnClickListener(
                view -> clickListener.onImageClick(item)
        );
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ImageViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView imageView;
        final TextView titleView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            titleView = itemView.findViewById(R.id.textViewTitle);
        }
    }
}
