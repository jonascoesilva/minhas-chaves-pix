package br.com.chavespix;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public final class PixCardAdapter extends RecyclerView.Adapter<PixCardAdapter.CardViewHolder> {
    public interface Listener {
        void onOpen(PixCard card);
    }

    private final Listener listener;
    private final List<PixCard> cards = new ArrayList<>();

    public PixCardAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setCards(List<PixCard> values) {
        cards.clear();
        cards.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pix_key_group, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        PixCard card = cards.get(position);
        holder.personName.setText(card.getBeneficiaryName());
        holder.bankName.setText(card.getBankName());
        holder.accent.setBackgroundColor(visibleAccentColor(holder.itemView, card.getColorArgb()));
        holder.keysContainer.removeAllViews();
        holder.itemView.setContentDescription(
                holder.itemView.getContext().getString(
                        R.string.open_card_description,
                        card.getBeneficiaryName(),
                        card.getBankName()
                )
        );
        holder.itemView.setOnClickListener(view -> listener.onOpen(card));

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (PixCard.KeyEntry key : card.getKeys()) {
            View row = inflater.inflate(
                    R.layout.item_pix_card_key_summary,
                    holder.keysContainer,
                    false
            );
            String label = PixKeyValidator.typeLabel(key.getType());
            ((TextView) row.findViewById(R.id.summary_type)).setText(label);
            TextView value = row.findViewById(R.id.summary_value);
            value.setText(PixCardDisplayFormatter.formatSummaryValue(
                    key.getType(),
                    key.getValue()
            ));
            if (PixKeyValidator.isRandomType(key.getType())) {
                value.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            } else if (PixKeyValidator.TYPE_EMAIL.equals(key.getType())) {
                value.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
            holder.keysContainer.addView(row);
        }
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    private static int visibleAccentColor(View view, int color) {
        int surface = ContextCompat.getColor(view.getContext(), R.color.surface_card);
        if (contrastRatio(color, surface) >= 3d) {
            return color;
        }
        int target = luminance(surface) > 0.5d ? Color.BLACK : Color.WHITE;
        for (float amount = 0.08f; amount <= 0.88f; amount += 0.08f) {
            int adjusted = Color.rgb(
                    blend(Color.red(color), Color.red(target), amount),
                    blend(Color.green(color), Color.green(target), amount),
                    blend(Color.blue(color), Color.blue(target), amount)
            );
            if (contrastRatio(adjusted, surface) >= 3d) {
                return adjusted;
            }
        }
        return target;
    }

    private static int blend(int source, int target, float amount) {
        return Math.round(source * (1f - amount) + target * amount);
    }

    private static double contrastRatio(int first, int second) {
        double one = luminance(first);
        double two = luminance(second);
        return (Math.max(one, two) + 0.05d) / (Math.min(one, two) + 0.05d);
    }

    private static double luminance(int color) {
        return 0.2126d * linear(Color.red(color))
                + 0.7152d * linear(Color.green(color))
                + 0.0722d * linear(Color.blue(color));
    }

    private static double linear(int component) {
        double value = component / 255d;
        return value <= 0.03928d
                ? value / 12.92d
                : Math.pow((value + 0.055d) / 1.055d, 2.4d);
    }

    static final class CardViewHolder extends RecyclerView.ViewHolder {
        final TextView personName;
        final TextView bankName;
        final View accent;
        final LinearLayout keysContainer;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            personName = itemView.findViewById(R.id.person_name);
            bankName = itemView.findViewById(R.id.card_bank);
            accent = itemView.findViewById(R.id.card_accent);
            keysContainer = itemView.findViewById(R.id.keys_container);
            ViewCompat.setAccessibilityDelegate(itemView, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(
                        View host,
                        AccessibilityNodeInfoCompat info
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(android.widget.Button.class.getName());
                }
            });
        }
    }
}
